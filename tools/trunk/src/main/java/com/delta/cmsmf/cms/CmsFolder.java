/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.delta.cmsmf.cms.CmsAttributeHandlers.AttributeHandler;
import com.documentum.com.DfClientX;
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
		AttributeHandler handler = new AttributeHandler() {
			@Override
			public boolean includeInImport(IDfPersistentObject object, CmsAttribute attribute) throws DfException {
				return false;
			}
		};
		// These are the attributes that require special handling on import
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING,
			CmsAttributes.R_FOLDER_PATH, handler);

		CmsFolder.HANDLERS_READY = true;
	}

	/**
	 * This DQL will find all users for which this folder is marked as the default folder,
	 * and thus all users for whom it must be restored later on.
	 */
	private static final String DQL_FIND_USERS_WITH_DEFAULT_FOLDER = "SELECT u.user_name, u.default_folder FROM dm_user u, dm_folder f WHERE any f.r_folder_path = u.default_folder AND f.r_object_id = ''%s''";

	public CmsFolder() {
		super(CmsObjectType.FOLDER, IDfFolder.class);
		CmsFolder.initHandlers();
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfFolder folder) throws DfException {
		final String folderId = folder.getObjectId().getId();

		IDfQuery dqlQry = new DfClientX().getQuery();
		dqlQry.setDQL(String.format(CmsFolder.DQL_FIND_USERS_WITH_DEFAULT_FOLDER, folderId));
		IDfCollection resultCol = dqlQry.execute(folder.getSession(), IDfQuery.EXEC_QUERY);
		CmsProperty usersWithDefaultFolder = null;
		CmsProperty usersDefaultFolderPaths = null;
		try {
			usersWithDefaultFolder = new CmsProperty(CmsFolder.USERS_WITH_DEFAULT_FOLDER, CmsDataType.DF_STRING);
			usersDefaultFolderPaths = new CmsProperty(CmsFolder.USERS_DEFAULT_FOLDER_PATHS, CmsDataType.DF_STRING);
			while (resultCol.next()) {
				usersWithDefaultFolder.addValue(resultCol.getValueAt(0));
				usersDefaultFolderPaths.addValue(resultCol.getValueAt(1));
			}
			properties.add(usersWithDefaultFolder);
			properties.add(usersDefaultFolderPaths);
		} finally {
			closeQuietly(resultCol);
		}
	}

	@Override
	protected void finalizeConstruction(IDfFolder folder, boolean newObject) throws DfException {

		// TODO: Link the folder to all its paths...?
		Set<String> actualPaths = new HashSet<String>();
		CmsAttribute folderPaths = getAttribute(CmsAttributes.R_FOLDER_PATH);
		if (folderPaths != null) {
			for (IDfValue v : folderPaths) {
				// TODO: Link the folder object to the given path
				// TODO: If the given path already exists...is it a different folder? What to do?
				actualPaths.add(v.asString());
			}
		}

		CmsProperty usersWithDefaultFolder = getProperty(CmsFolder.USERS_WITH_DEFAULT_FOLDER);
		CmsProperty usersDefaultFolderPaths = getProperty(CmsFolder.USERS_DEFAULT_FOLDER_PATHS);
		if ((usersWithDefaultFolder != null) && (usersDefaultFolderPaths != null)) {
			final IDfSession session = folder.getSession();
			final int total = usersWithDefaultFolder.getValueCount();
			for (int i = 0; i < total; i++) {
				IDfValue userValue = usersWithDefaultFolder.getValue(i);
				IDfValue pathValue = usersDefaultFolderPaths.getValue(i);

				// TODO: How do we decide if we should update the default folder for this user? What
				// if the user's default folder has been modified on the target CMS and we don't
				// want to clobber that?
				final IDfUser user = session.getUser(userValue.asString());
				if (user == null) {
					this.logger
					.warn(String
						.format(
							"Failed to link Folder [%s] to user [%s] as its default folder - the user wasn't found - probably didn't need to be copied over",
							folder.getObjectId().getId(), userValue.asString()));
					continue;
				}

				// Ok...so...is the user's default path one of ours? Or is it perhaps a different
				// object? This will determine if the user's default folder will be private or
				// not...
				IDfFolder actual = folder;
				if (!actualPaths.contains(pathValue.asString())) {
					// Not one of ours
					actual = session.getFolderByPath(pathValue.asString());
				}
				// Ok...so...we set the path to "whatever"...
				user.setDefaultFolder(pathValue.asString(), (actual == null));
			}
		}
	}

	@Override
	protected IDfFolder locateInCms(IDfSession session) throws DfException {
		return null;
	}
}