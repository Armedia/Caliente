/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

import java.util.Map;
import java.util.TreeMap;

import org.apache.chemistry.opencmis.commons.PropertyIds;

import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.common.DctmFolder;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfType;
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

	public DctmImportFolder(DctmImportDelegateFactory factory, CmfObject<IDfValue> storedObject) throws Exception {
		super(factory, IDfFolder.class, DctmObjectType.FOLDER, storedObject);
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
	protected void doFinalizeConstruction(IDfFolder folder, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {

		final String folderName;
		if (newObject) {
			CmfAttribute<IDfValue> objectName = this.cmfObject.getAttribute(DctmAttributes.OBJECT_NAME);
			IDfValue newValue = objectName.getValue();
			folderName = newValue.toString().trim();
			setAttributeOnObject(DctmAttributes.OBJECT_NAME, DfValueFactory.newStringValue(folderName), folder);
		} else {
			folderName = folder.getObjectName();
		}

		// Only do the linking/unlinking for non-cabinets
		CmfProperty<IDfValue> p = this.cmfObject.getProperty(PropertyIds.PARENT_ID);
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
		final CmfProperty<IDfValue> usersWithDefaultFolder = this.cmfObject
			.getProperty(DctmFolder.USERS_WITH_DEFAULT_FOLDER);
		final CmfProperty<IDfValue> usersDefaultFolderPaths = this.cmfObject
			.getProperty(DctmFolder.USERS_DEFAULT_FOLDER_PATHS);

		if ((usersWithDefaultFolder == null) || (usersDefaultFolderPaths == null)
			|| (usersWithDefaultFolder.getValueCount() == 0) || (usersDefaultFolderPaths.getValueCount() == 0)) { return; }

		final int total = usersWithDefaultFolder.getValueCount();
		Map<String, String> m = new TreeMap<String, String>();
		for (int i = 0; i < total; i++) {
			String user = usersWithDefaultFolder.getValue(i).asString();
			// Don't touch the special users!!
			if (context.isUntouchableUser(user)) {
				this.log.warn(String.format("Will not substitute the default folder for the special user [%s]",
					DctmMappingUtils.resolveMappableUser(session, user)));
				continue;
			}
			m.put(user, usersDefaultFolderPaths.getValue(i).asString());
		}

		for (Map.Entry<String, String> entry : m.entrySet()) {
			final String actualUser = entry.getKey();
			final String pathValue = context.getTargetPath(entry.getValue());

			// TODO: How do we decide if we should update the default folder for this user? What
			// if the user's default folder has been modified on the target CMS and we don't
			// want to clobber that? That's a decision that needs to be made later...
			final IDfUser user;
			try {
				user = DctmImportUser.locateExistingUser(context, actualUser);
			} catch (MultipleUserMatchesException e) {
				String msg = String.format("Failed to link folder [%s](%s) to user [%s] as its default folder - %s",
					this.cmfObject.getLabel(), folder.getObjectId().getId(), actualUser, e.getMessage());
				if (context.isSupported(CmfType.USER)) { throw new ImportException(msg); }
				this.log.warn(msg);
				continue;
			}
			if (user == null) {
				String msg = String
					.format(
						"Failed to link folder [%s](%s) to user [%s] as its default folder - the user wasn't found - probably didn't need to be copied over",
						this.cmfObject.getLabel(), folder.getObjectId().getId(), actualUser);
				if (context.isSupported(CmfType.USER)) { throw new ImportException(msg); }
				this.log.warn(msg);
				continue;
			}

			// Ok...so...is the user's default path one of ours? Or is it perhaps a different
			// object? This will determine if the user's default folder will be private or
			// not...
			IDfFolder actual = session.getFolderByPath(pathValue);

			// Ok...so...we set the path to "whatever"...
			DfUtils.lockObject(this.log, user);
			user.fetch(null);
			user.setDefaultFolder(pathValue, (actual == null));
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
								actualUser, this.cmfObject.getLabel()), e);
			}
		}
	}

	@Override
	protected IDfFolder locateInCms(DctmImportContext ctx) throws ImportException, DfException {
		// If I'm a cabinet, then find it by cabinet name
		IDfSession session = ctx.getSession();
		// Easier way: determine if we have parent folders...if not, then we're a cabinet
		CmfProperty<IDfValue> p = this.cmfObject.getProperty(PropertyIds.PARENT_ID);
		if ((p == null) || !p.hasValues()) {
			// This is a cabinet...
			return session.getFolderByPath(String.format("/%s", this.cmfObject.getAttribute(DctmAttributes.OBJECT_NAME)
				.getValue().asString()));
		}
		return super.locateInCms(ctx);
	}

	@Override
	protected IDfFolder newObject(DctmImportContext ctx) throws DfException, ImportException {
		CmfProperty<IDfValue> p = this.cmfObject.getProperty(PropertyIds.PARENT_ID);
		if ((p == null) || !p.hasValues()) {
			IDfFolder newObject = castObject(ctx.getSession().newObject("dm_cabinet"));
			setOwnerGroupACLData(newObject, ctx);
			return newObject;
		}
		return super.newObject(ctx);
	}
}