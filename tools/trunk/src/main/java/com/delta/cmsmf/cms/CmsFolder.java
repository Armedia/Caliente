/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.mainEngine.RepositoryConfiguration;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsFolder extends CmsObject<IDfFolder> {

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

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfFolder folder) throws DfException {
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
					.addValue(CmsMappingUtils.substituteSpecialUsers(folder, resultCol.getValueAt(0)));
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
		String owner = CmsMappingUtils.resolveSpecialUser(session, folder.getOwnerName());
		if (!CmsMappingUtils.isSpecialUserSubstitution(owner)) {
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
		String storageType = folder.getStorageType();
		if (StringUtils.isNotBlank(storageType)) {
			RepositoryConfiguration.getRepositoryConfiguration().addFileStore(storageType);
		}
	}

	private PermitDelta mainPermitDelta = null;
	private Map<String, PermitDelta> parentPermitDeltas = null;

	@Override
	protected void prepareForConstruction(IDfFolder folder, boolean newObject, CmsTransferContext context)
		throws DfException {
		this.mainPermitDelta = null;
		this.parentPermitDeltas = null;

		// If updating an existing folder object, make sure that you have write
		// permissions and CHANGE_LOCATION. If you don't, grant them, and reset it later on.
		if (!newObject) {
			this.mainPermitDelta = new PermitDelta(folder, IDfACL.DF_PERMIT_WRITE,
				IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR);
			if (this.mainPermitDelta.grant(folder)) {
				folder.save();
			}
		}
	}

	@Override
	protected void finalizeConstruction(IDfFolder folder, boolean newObject, CmsTransferContext context)
		throws DfException {

		final String folderName;
		if (newObject) {
			CmsAttribute objectName = getAttribute(CmsAttributes.OBJECT_NAME);
			IDfValue newValue = objectName.getValue();
			folderName = newValue.toString().trim();
			setAttributeOnObject(CmsAttributes.OBJECT_NAME, DfValueFactory.newStringValue(folderName), folder);
		} else {
			folderName = folder.getObjectName();
		}

		final IDfSession session = folder.getSession();

		// Only do the linking/unlinking for non-cabinets
		Set<String> actualPaths = new TreeSet<String>();
		if (!"dm_cabinet".equals(getSubtype())) {

			Map<String, IDfFolder> oldParents = new HashMap<String, IDfFolder>();
			int oldParentCount = folder.getFolderPathCount();
			for (int i = 0; i < oldParentCount; i++) {
				String path = folder.getFolderPath(i);
				IDfFolder parent = session.getFolderByPath(path.substring(0, path.lastIndexOf("/")));
				if (parent != null) {
					oldParents.put(parent.getObjectId().getId(), parent);
				}
			}

			Map<String, IDfFolder> newParents = new HashMap<String, IDfFolder>();
			for (IDfValue v : getAttribute(CmsAttributes.R_FOLDER_PATH)) {
				String path = v.asString();
				IDfFolder parent = session.getFolderByPath(path.substring(0, path.lastIndexOf("/")));
				if (parent != null) {
					newParents.put(parent.getObjectId().getId(), parent);
				}
			}

			// Unlink from those who are in the old parent list, but not in the new parent list
			Set<String> unlinkTargets = new HashSet<String>(oldParents.keySet());
			unlinkTargets.removeAll(newParents.keySet());
			for (String oldParentId : unlinkTargets) {
				IDfFolder parent = oldParents.get(oldParentId);
				PermitDelta delta = new PermitDelta(parent, IDfACL.DF_PERMIT_WRITE,
					IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR);
				if (delta.grant(parent)) {
					parent.save();
				}
				folder.unlink(oldParentId);
				if (delta.revoke(parent)) {
					parent.save();
				}
			}

			// Link to those who are in the new parent list, but not the old parent list
			Set<String> linkTargets = new HashSet<String>(newParents.keySet());
			linkTargets.removeAll(oldParents.keySet());
			this.parentPermitDeltas = new HashMap<String, PermitDelta>();
			for (String parentId : newParents.keySet()) {
				IDfFolder parent = newParents.get(parentId);
				// If we should link here, then link!
				if (linkTargets.contains(parentId)) {
					PermitDelta delta = new PermitDelta(parent, IDfACL.DF_PERMIT_WRITE,
						IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR);
					this.parentPermitDeltas.put(parentId, delta);
					if (delta.grant(parent)) {
						parent.save();
					}

					folder.link(parentId);
				}

				// Keep track of the paths
				int pathCount = parent.getFolderPathCount();
				for (int i = 0; i < pathCount; i++) {
					String newPath = String.format("%s/%s", parent.getFolderPath(i), folderName);
					actualPaths.add(newPath);
				}
			}
		} else {
			actualPaths.add(String.format("/%s", folderName));
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

		final IDfSession session = folder.getSession();

		CmsProperty usersWithDefaultFolder = getProperty(CmsFolder.USERS_WITH_DEFAULT_FOLDER);
		CmsProperty usersDefaultFolderPaths = getProperty(CmsFolder.USERS_DEFAULT_FOLDER_PATHS);
		if ((usersWithDefaultFolder != null) && (usersDefaultFolderPaths != null)) {
			final int total = usersWithDefaultFolder.getValueCount();
			for (int i = 0; i < total; i++) {
				IDfValue userValue = usersWithDefaultFolder.getValue(i);
				IDfValue pathValue = usersDefaultFolderPaths.getValue(i);

				if (CmsMappingUtils.isSpecialUserSubstitution(userValue.asString())) {
					this.log.warn(String.format("Will not substitute the default folder for the special user [%s]",
						CmsMappingUtils.resolveSpecialUser(session, userValue.asString())));
					continue;
				}

				// TODO: How do we decide if we should update the default folder for this user? What
				// if the user's default folder has been modified on the target CMS and we don't
				// want to clobber that? That's a decision that needs to be made later...
				final IDfUser user = session.getUser(userValue.asString());
				if (user == null) {
					this.log
					.warn(String
						.format(
							"Failed to link Folder [%s:%s] to user [%s] as its default folder - the user wasn't found - probably didn't need to be copied over",
							folder.getObjectId().getId(), getLabel(), userValue.asString()));
					continue;
				}

				// Ok...so...is the user's default path one of ours? Or is it perhaps a different
				// object? This will determine if the user's default folder will be private or
				// not...
				IDfFolder actual = session.getFolderByPath(pathValue.asString());

				// Ok...so...we set the path to "whatever"...
				user.setDefaultFolder(pathValue.asString(), (actual == null));
				user.save();
			}
		}
		return newObject;
	}

	@Override
	protected boolean cleanupAfterSave(IDfFolder folder, boolean newObject, CmsTransferContext context)
		throws DfException {

		boolean changed = false;
		if (this.mainPermitDelta != null) {
			changed = this.mainPermitDelta.revoke(folder);
		}

		if (this.parentPermitDeltas != null) {
			IDfSession session = folder.getSession();
			for (String parentId : this.parentPermitDeltas.keySet()) {
				IDfFolder parent = session.getFolderBySpecification(parentId);
				if (parent == null) {
					// Again...how the hell?
					continue;
				}
				if (this.parentPermitDeltas.get(parentId).revoke(parent)) {
					parent.save();
				}
			}
		}

		// Return true so this object is saved afterwards
		return changed;
	}

	@Override
	protected IDfFolder locateInCms(CmsTransferContext ctx) throws CMSMFException, DfException {
		IDfFolder existing = null;
		String existingPath = null;
		final IDfSession session = ctx.getSession();
		for (IDfValue path : getAttribute(CmsAttributes.R_FOLDER_PATH)) {
			String currentPath = path.asString();
			IDfFolder current = session.getFolderByPath(currentPath);
			if (current == null) {
				// No match, we're good...
				continue;
			}
			// We have a match...
			if (existing == null) {
				// First match, keep track of it
				existing = current;
				existingPath = currentPath;
				continue;
			}
			// Second match, is it the same as the first?
			if (Tools.equals(existing.getObjectId().getId(), current.getObjectId().getId())) {
				// Same as the first - we have an issue here
				continue;
			}
			// Not the same, this is a problem
			throw new CMSMFException(String.format(
				"Found two different folders matching this folder's paths: [%s@%s] and [%s@%s]", existing.getObjectId()
				.getId(), existingPath, current.getObjectId().getId(), currentPath));
		}
		return existing;
	}
}