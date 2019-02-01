/**
 *
 */

package com.armedia.caliente.engine.dfc.importer;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmMappingUtils;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.DctmTranslator;
import com.armedia.caliente.engine.dfc.common.DctmFolder;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.utilities.FileNameTools;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
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
		if (newObject && !isDfReference(folder)) {
			CmfAttribute<IDfValue> objectName = this.cmfObject.getAttribute(DctmAttributes.OBJECT_NAME);
			setAttributeOnObject(DctmAttributes.OBJECT_NAME, objectName.getValue(), folder);
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
			.getProperty(IntermediateProperty.USERS_WITH_DEFAULT_FOLDER);
		final CmfProperty<IDfValue> usersDefaultFolderPaths = this.cmfObject
			.getProperty(IntermediateProperty.USERS_DEFAULT_FOLDER_PATHS);

		if ((usersWithDefaultFolder == null) || (usersDefaultFolderPaths == null)
			|| (usersWithDefaultFolder.getValueCount() == 0) || (usersDefaultFolderPaths.getValueCount() == 0)) {
			return;
		}

		final int total = usersWithDefaultFolder.getValueCount();
		Map<String, String> m = new TreeMap<>();
		for (int i = 0; i < total; i++) {
			String user = usersWithDefaultFolder.getValue(i).asString();
			// Don't touch the special users!!
			if (context.isUntouchableUser(user)) {
				this.log.warn("Will not substitute the default folder for the special user [{}]",
					DctmMappingUtils.resolveMappableUser(session, user));
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
				this.log.warn(msg);
				continue;
			}
			if (user == null) {
				String msg = String.format(
					"Failed to link folder [%s](%s) to user [%s] as its default folder - the user wasn't found - probably didn't need to be copied over",
					this.cmfObject.getLabel(), folder.getObjectId().getId(), actualUser);
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
				this.log.warn(
					"Failed to update the system attributes for user [{}] after assigning folder [{}] as their default folder",
					actualUser, this.cmfObject.getLabel(), e);
			}
		}

		final CmfProperty<IDfValue> groupsWithDefaultFolder = this.cmfObject
			.getProperty(IntermediateProperty.GROUPS_WITH_DEFAULT_FOLDER);
		if ((groupsWithDefaultFolder != null) && groupsWithDefaultFolder.hasValues()) {
			Set<String> s = new TreeSet<>();
			for (IDfValue v : groupsWithDefaultFolder) {
				s.add(v.asString());
			}
			for (String g : s) {
				IDfGroup group = session.getGroup(g);
				if (group == null) {
					String msg = String.format(
						"Failed to link folder [%s](%s) to group [%s] as its default folder - the group wasn't found - probably didn't need to be copied over",
						this.cmfObject.getLabel(), folder.getObjectId().getId(), g);
					this.log.warn(msg);
					continue;
				}

				// It WAS a group! Set its group directory
				DfUtils.lockObject(this.log, group);
				group.fetch(null);
				group.setGroupDirectoryId(folder.getObjectId());
				group.save();

				// Update the system attributes, if we can
				try {
					updateSystemAttributes(group, context);
				} catch (ImportException e) {
					this.log.warn(
						"Failed to update the system attributes for group [{}] after assigning folder [{}] as its default folder",
						g, this.cmfObject.getLabel(), e);
				}
			}
		}
	}

	@Override
	protected IDfFolder locateInCms(DctmImportContext ctx) throws ImportException, DfException {
		// If I'm a cabinet, then find it by cabinet name
		IDfSession session = ctx.getSession();
		// Easier way: determine if we have parent folders...if not, then we're a cabinet
		CmfProperty<IDfValue> p = this.cmfObject.getProperty(IntermediateProperty.PARENT_ID);
		if ((p == null) || !p.hasValues()) {
			// This is a cabinet...
			String path = String.format("/%s",
				this.cmfObject.getAttribute(DctmAttributes.OBJECT_NAME).getValue().asString());
			return session.getFolderByPath(ctx.getTargetPath(path));
		}
		return super.locateInCms(ctx);
	}

	@Override
	protected IDfFolder newObject(DctmImportContext ctx) throws DfException, ImportException {
		if (isReference()) { return newReference(ctx); }

		CmfProperty<IDfValue> p = this.cmfObject.getProperty(IntermediateProperty.PATH);
		CmfAttribute<IDfValue> a = this.cmfObject.getAttribute(DctmAttributes.OBJECT_NAME);
		String path = "";
		final String name = a.getValue().asString();
		if ((p != null) && p.hasValues()) {
			path = p.getValue().asString();
		}
		path = ctx.getTargetPath(String.format("%s/%s", path, name));

		if ("/".equals(FileNameTools.dirname(path, '/'))) {
			// TODO: Try to identify if the object's type is a cabinet subtype. If it is,
			// then we don't need to modify it
			String typeName = "dm_cabinet";
			IDfType type = DctmTranslator.translateType(ctx, this.cmfObject);
			if ((type != null) && type.isTypeOf("dm_cabinet")) {
				typeName = type.getName();
			}
			IDfFolder newObject = castObject(ctx.getSession().newObject(typeName));
			setOwnerGroupACLData(newObject, ctx);
			return newObject;
		}
		return super.newObject(ctx);
	}
}