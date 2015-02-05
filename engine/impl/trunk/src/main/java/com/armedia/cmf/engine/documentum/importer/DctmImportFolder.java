/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.common.DctmFolder;
import com.armedia.cmf.engine.documentum.common.DctmSysObject;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmImportFolder extends DctmImportSysObject<IDfFolder> implements DctmFolder {

	public DctmImportFolder(DctmImportEngine engine, StoredObject<IDfValue> storedObject) {
		super(engine, DctmObjectType.FOLDER, storedObject);
	}

	@Override
	protected String calculateLabel(IDfFolder folder) throws DfException {
		return folder.getFolderPath(0);
	}

	private TemporaryPermission mainTemporaryPermission = null;

	@Override
	protected void prepareForConstruction(IDfFolder folder, boolean newObject, DctmImportContext context)
		throws DfException {
		this.mainTemporaryPermission = null;

		// If updating an existing folder object, make sure that you have write
		// permissions and CHANGE_LOCATION. If you don't, grant them, and reset it later on.
		if (!newObject) {
			this.mainTemporaryPermission = new TemporaryPermission(folder, IDfACL.DF_PERMIT_DELETE,
				IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR);
			if (this.mainTemporaryPermission.grant(folder)) {
				folder.save();
			}
		}
	}

	@Override
	protected void finalizeConstruction(IDfFolder folder, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {

		final String folderName;
		if (newObject) {
			StoredAttribute<IDfValue> objectName = this.storedObject.getAttribute(DctmAttributes.OBJECT_NAME);
			IDfValue newValue = objectName.getValue();
			folderName = newValue.toString().trim();
			setAttributeOnObject(DctmAttributes.OBJECT_NAME, DfValueFactory.newStringValue(folderName), folder);
		} else {
			folderName = folder.getObjectName();
		}

		// Only do the linking/unlinking for non-cabinets
		StoredProperty<IDfValue> p = this.storedObject.getProperty(DctmSysObject.TARGET_PATHS);
		if ((p != null) && p.hasValues()) {
			linkToParents(folder, context);
		}
	}

	@Override
	protected boolean postConstruction(IDfFolder folder, boolean newObject, DctmImportContext context)
		throws DfException {
		if (newObject) {
			// Make sure we apply the original name, since we had to "massage" it to
			// get things to work later on
			copyAttributeToObject(DctmAttributes.OBJECT_NAME, folder);
		}

		if (this.mainTemporaryPermission != null) {
			newObject |= this.mainTemporaryPermission.revoke(folder);
		}
		return newObject;
	}

	@Override
	protected boolean cleanupAfterSave(IDfFolder folder, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
		cleanUpParents(folder.getSession());
		return super.cleanupAfterSave(folder, newObject, context);
	}

	@Override
	protected void updateReferenced(IDfFolder folder, DctmImportContext context) throws DfException, ImportException {
		final IDfSession session = context.getSession();
		final StoredProperty<IDfValue> usersWithDefaultFolder = this.storedObject
			.getProperty(DctmFolder.USERS_WITH_DEFAULT_FOLDER);
		final StoredProperty<IDfValue> usersDefaultFolderPaths = this.storedObject
			.getProperty(DctmFolder.USERS_DEFAULT_FOLDER_PATHS);

		if ((usersWithDefaultFolder == null) || (usersDefaultFolderPaths == null)
			|| (usersWithDefaultFolder.getValueCount() == 0) || (usersDefaultFolderPaths.getValueCount() == 0)) { return; }

		final int total = usersWithDefaultFolder.getValueCount();
		for (int i = 0; i < total; i++) {
			IDfValue userValue = usersWithDefaultFolder.getValue(i);
			IDfValue pathValue = usersDefaultFolderPaths.getValue(i);

			if (DctmMappingUtils.isSubstitutionForMappableUser(userValue.asString())) {
				this.log.warn(String.format("Will not substitute the default folder for the special user [%s]",
					DctmMappingUtils.resolveMappableUser(session, userValue.asString())));
				continue;
			}

			// TODO: How do we decide if we should update the default folder for this user? What
			// if the user's default folder has been modified on the target CMS and we don't
			// want to clobber that? That's a decision that needs to be made later...
			final String actualUser = userValue.asString();
			final IDfUser user = session.getUser(actualUser);
			if (user == null) {
				this.log
					.warn(String
						.format(
							"Failed to link Folder [%s](%s) to user [%s] as its default folder - the user wasn't found - probably didn't need to be copied over",
							this.storedObject.getLabel(), folder.getObjectId().getId(), actualUser));
				continue;
			}

			// Ok...so...is the user's default path one of ours? Or is it perhaps a different
			// object? This will determine if the user's default folder will be private or
			// not...
			IDfFolder actual = session.getFolderByPath(pathValue.asString());

			// Ok...so...we set the path to "whatever"...
			user.lock();
			user.fetch(null);
			user.setDefaultFolder(pathValue.asString(), (actual == null));
			user.save();
			// Update the system attributes, if we can
			try {
				updateSystemAttributes(user, context);
			} catch (ImportException e) {
				this.log
					.warn(
						String
							.format(
								"Failed to update the system attributes for user [%s] after assigning folder [%s] as their default folder",
								actualUser, this.storedObject.getLabel()), e);
			}
		}
	}

	@Override
	protected IDfFolder locateInCms(DctmImportContext ctx) throws ImportException, DfException {
		// If I'm a cabinet, then find it by cabinet name
		IDfSession session = ctx.getSession();
		// Easier way: determine if we have parent folders...if not, then we're a cabinet
		StoredProperty<IDfValue> p = this.storedObject.getProperty(DctmSysObject.TARGET_PATHS);
		if ((p == null) || !p.hasValues()) {
			// This is a cabinet...
			return session.getFolderByPath(String.format("/%s",
				this.storedObject.getAttribute(DctmAttributes.OBJECT_NAME).getValue().asString()));
		}
		return super.locateInCms(ctx);
	}

	@Override
	protected IDfFolder newObject(DctmImportContext ctx) throws DfException, ImportException {
		StoredProperty<IDfValue> p = this.storedObject.getProperty(DctmSysObject.TARGET_PATHS);
		if ((p == null) || !p.hasValues()) { return castObject(ctx.getSession().newObject("dm_cabinet")); }
		return super.newObject(ctx);
	}
}