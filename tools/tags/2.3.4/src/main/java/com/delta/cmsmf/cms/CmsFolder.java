/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CmsFolder extends CmsSysObject<IDfFolder> {

	private static final String USERS_WITH_DEFAULT_FOLDER = "usersWithDefaultFolder";
	private static final String USERS_DEFAULT_FOLDER_PATHS = "usersDefaultFolderPaths";

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (CmsFolder.HANDLERS_READY) { return; }
		// These are the attributes that require special handling on import
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.FOLDER, CmsDataType.DF_STRING,
			CmsAttributes.R_FOLDER_PATH, CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.FOLDER, CmsDataType.DF_STRING,
			CmsAttributes.OBJECT_NAME, CmsAttributeHandlers.NO_IMPORT_HANDLER);

		// These attributes can be substituted for values
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.FOLDER, CmsDataType.DF_STRING, CmsAttributes.OWNER_NAME,
			CmsAttributeHandlers.SESSION_CONFIG_USER_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.FOLDER, CmsDataType.DF_STRING, CmsAttributes.ACL_DOMAIN,
			CmsAttributeHandlers.SESSION_CONFIG_USER_HANDLER);

		CmsFolder.HANDLERS_READY = true;
	}

	/**
	 * This DQL will find all users for which this folder is marked as the default folder, and thus
	 * all users for whom it must be restored later on.
	 */
	private static final String DQL_FIND_USERS_WITH_DEFAULT_FOLDER = "SELECT u.user_name, u.default_folder FROM dm_user u, dm_folder f WHERE any f.r_folder_path = u.default_folder AND f.r_object_id = '%s'";

	public CmsFolder() {
		super(IDfFolder.class);
		CmsFolder.initHandlers();
	}

	@Override
	protected String calculateLabel(IDfFolder folder) throws DfException {
		return folder.getFolderPath(0);
	}

	private int calculateDepth(IDfSession session, IDfId folderId, Set<String> visited) throws DfException {
		// If the folder has already been visited, we have a loop...so let's explode loudly
		if (visited.contains(folderId.getId())) { throw new DfException(String.format(
			"Folder loop detected, element [%s] exists twice: %s", folderId.getId(), visited.toString())); }
		visited.add(folderId.getId());
		try {
			int depth = 0;
			String dql = String.format("select i_folder_id from dm_sysobject where r_object_id = '%s'",
				folderId.getId());
			IDfCollection results = DfUtils.executeQuery(session, dql, IDfQuery.DF_EXECREAD_QUERY);
			try {
				while (results.next()) {
					// My depth is the maximum depth from any of my parents, plus one
					IDfId parentId = results.getId(CmsAttributes.I_FOLDER_ID);
					if (parentId.isNull() || !parentId.isObjectId()) {
						continue;
					}
					depth = Math.max(depth, calculateDepth(session, parentId, visited) + 1);
				}
			} finally {
				DfUtils.closeQuietly(results);
			}
			return depth;
		} finally {
			visited.remove(folderId.getId());
		}
	}

	@Override
	protected String calculateBatchId(IDfFolder folder) throws DfException {
		// Calculate the maximum depth that this folder resides in, from its parents.
		// Keep track of visited nodes, and explode on a loop.
		Set<String> visited = new LinkedHashSet<String>();
		int depth = calculateDepth(folder.getSession(), folder.getObjectId(), visited);
		// We return it in zero-padded hex to allow for large numbers (up to 2^64
		// depth), and also maintain consistent sorting
		return String.format("%016x", depth);
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfFolder folder) throws DfException,
		CMSMFException {
		super.getDataProperties(properties, folder);
		final String folderId = folder.getObjectId().getId();

		IDfCollection resultCol = DfUtils.executeQuery(folder.getSession(),
			String.format(CmsFolder.DQL_FIND_USERS_WITH_DEFAULT_FOLDER, folderId), IDfQuery.DF_EXECREAD_QUERY);
		CmsProperty usersWithDefaultFolder = null;
		CmsProperty usersDefaultFolderPaths = null;
		try {
			usersWithDefaultFolder = new CmsProperty(CmsFolder.USERS_WITH_DEFAULT_FOLDER, CmsDataType.DF_STRING);
			usersDefaultFolderPaths = new CmsProperty(CmsFolder.USERS_DEFAULT_FOLDER_PATHS, CmsDataType.DF_STRING);
			while (resultCol.next()) {
				// TODO: This probably should not be done for special users
				usersWithDefaultFolder
					.addValue(CmsMappingUtils.substituteMappableUsers(folder, resultCol.getValueAt(0)));
				usersDefaultFolderPaths.addValue(resultCol.getValueAt(1));
			}
			properties.add(usersWithDefaultFolder);
			properties.add(usersDefaultFolderPaths);
		} finally {
			DfUtils.closeQuietly(resultCol);
		}
	}

	@Override
	protected void doPersistRequirements(IDfFolder folder, CmsTransferContext ctx,
		CmsDependencyManager dependencyManager) throws DfException, CMSMFException {
		final IDfSession session = folder.getSession();
		// The parent folders
		final int pathCount = folder.getFolderPathCount();
		for (int i = 0; i < pathCount; i++) {
			String path = folder.getFolderPath(i);
			String parentPath = path.substring(0, path.lastIndexOf("/"));
			if (StringUtils.isNotBlank(parentPath)) {
				IDfFolder parent = session.getFolderByPath(parentPath);
				if (parent != null) {
					dependencyManager.persistRelatedObject(parent);
				}
			}
		}
	}

	@Override
	protected void doPersistDependents(IDfFolder folder, CmsTransferContext ctx, CmsDependencyManager dependencyManager)
		throws DfException, CMSMFException {

		final IDfSession session = folder.getSession();
		String owner = CmsMappingUtils.resolveMappableUser(session, folder.getOwnerName());
		if (!CmsMappingUtils.isSubstitutionForMappableUser(owner)) {
			IDfUser user = session.getUser(folder.getOwnerName());
			if (user != null) {
				dependencyManager.persistRelatedObject(user);
			}
		}

		// Do the others
		IDfPersistentObject[] dep = {
			// The group
			session.getGroup(folder.getGroupName()),
			// The ACL
			folder.getACL()
		};
		for (IDfPersistentObject obj : dep) {
			if (obj == null) {
				continue;
			}
			dependencyManager.persistRelatedObject(obj);
		}

		// Export the object type
		dependencyManager.persistRelatedObject(folder.getType());

		// Save filestore name
		// String storageType = folder.getStorageType();
		// if (StringUtils.isNotBlank(storageType)) {
		// RepositoryConfiguration.getRepositoryConfiguration().addFileStore(storageType);
		// }
	}

	private TemporaryPermission mainTemporaryPermission = null;

	@Override
	protected void prepareForConstruction(IDfFolder folder, boolean newObject, CmsTransferContext context)
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
	protected void finalizeConstruction(IDfFolder folder, boolean newObject, CmsTransferContext context)
		throws DfException, CMSMFException {

		final String folderName;
		if (newObject) {
			CmsAttribute objectName = getAttribute(CmsAttributes.OBJECT_NAME);
			IDfValue newValue = objectName.getValue();
			folderName = newValue.toString().trim();
			setAttributeOnObject(CmsAttributes.OBJECT_NAME, DfValueFactory.newStringValue(folderName), folder);
		} else {
			folderName = folder.getObjectName();
		}

		// Only do the linking/unlinking for non-cabinets
		if (!folder.getType().isTypeOf("dm_cabinet")) {
			linkToParents(folder, context);
		}
	}

	@Override
	protected boolean postConstruction(IDfFolder folder, boolean newObject, CmsTransferContext context)
		throws DfException {
		if (newObject) {
			// Make sure we apply the original name, since we had to "massage" it to
			// get things to work later on
			copyAttributeToObject(CmsAttributes.OBJECT_NAME, folder);
		}

		if (this.mainTemporaryPermission != null) {
			newObject |= this.mainTemporaryPermission.revoke(folder);
		}
		return newObject;
	}

	@Override
	protected boolean cleanupAfterSave(IDfFolder folder, boolean newObject, CmsTransferContext context)
		throws DfException, CMSMFException {
		cleanUpParents(folder.getSession());
		return super.cleanupAfterSave(folder, newObject, context);
	}

	@Override
	protected void updateReferenced(IDfFolder folder, CmsTransferContext context) throws DfException {
		final CmsProperty usersWithDefaultFolder = getProperty(CmsFolder.USERS_WITH_DEFAULT_FOLDER);
		final CmsProperty usersDefaultFolderPaths = getProperty(CmsFolder.USERS_DEFAULT_FOLDER_PATHS);
		if ((usersWithDefaultFolder == null) || (usersDefaultFolderPaths == null)
			|| (usersWithDefaultFolder.getValueCount() == 0) || (usersDefaultFolderPaths.getValueCount() == 0)) { return; }

		final IDfSession session = folder.getSession();
		final int total = usersWithDefaultFolder.getValueCount();
		for (int i = 0; i < total; i++) {
			IDfValue userValue = usersWithDefaultFolder.getValue(i);
			IDfValue pathValue = usersDefaultFolderPaths.getValue(i);

			if (CmsMappingUtils.isSubstitutionForMappableUser(userValue.asString())) {
				this.log.warn(String.format("Will not substitute the default folder for the special user [%s]",
					CmsMappingUtils.resolveMappableUser(session, userValue.asString())));
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
							getLabel(), folder.getObjectId().getId(), actualUser));
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
			} catch (CMSMFException e) {
				this.log
					.warn(
						String
							.format(
								"Failed to update the system attributes for user [%s] after assigning folder [%s] as their default folder",
								actualUser, getLabel()), e);
			}
		}
	}

	@Override
	protected IDfFolder locateInCms(CmsTransferContext ctx) throws CMSMFException, DfException {
		// If I'm a cabinet, then find it by cabinet name
		IDfSession session = ctx.getSession();
		IDfType t = session.getType(getSubtype());
		if (t.isTypeOf("dm_cabinet")) { return session.getFolderByPath(String.format("/%s",
			getAttribute(CmsAttributes.OBJECT_NAME).getValue().asString())); }
		return super.locateInCms(ctx);
	}
}