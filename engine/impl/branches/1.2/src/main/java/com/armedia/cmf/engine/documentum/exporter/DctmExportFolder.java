/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.common.DctmFolder;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportFolder extends DctmExportSysObject<IDfFolder> implements DctmFolder {

	/**
	 * This DQL will find all users for which this folder is marked as the default folder, and thus
	 * all users for whom it must be restored later on.
	 */
	private static final String DQL_FIND_USERS_WITH_DEFAULT_FOLDER = "SELECT u.user_name, u.default_folder FROM dm_user u, dm_folder f WHERE any f.r_folder_path = u.default_folder AND f.r_object_id = '%s'";

	protected DctmExportFolder(DctmExportEngine engine) {
		super(engine, DctmObjectType.FOLDER);
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfFolder folder) throws DfException {
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
					IDfId parentId = results.getId(DctmAttributes.I_FOLDER_ID);
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
	protected String calculateBatchId(IDfSession session, IDfFolder folder) throws DfException {
		// Calculate the maximum depth that this folder resides in, from its parents.
		// Keep track of visited nodes, and explode on a loop.
		Set<String> visited = new LinkedHashSet<String>();
		int depth = calculateDepth(session, folder.getObjectId(), visited);
		// We return it in zero-padded hex to allow for large numbers (up to 2^64
		// depth), and also maintain consistent sorting
		return String.format("%016x", depth);
	}

	@Override
	protected void getDataProperties(DctmExportContext ctx, Collection<StoredProperty<IDfValue>> properties,
		IDfFolder folder) throws DfException, ExportException {
		super.getDataProperties(ctx, properties, folder);
		final String folderId = folder.getObjectId().getId();

		IDfCollection resultCol = DfUtils.executeQuery(folder.getSession(),
			String.format(DctmExportFolder.DQL_FIND_USERS_WITH_DEFAULT_FOLDER, folderId), IDfQuery.DF_EXECREAD_QUERY);
		StoredProperty<IDfValue> usersWithDefaultFolder = null;
		StoredProperty<IDfValue> usersDefaultFolderPaths = null;
		try {
			usersWithDefaultFolder = new StoredProperty<IDfValue>(DctmFolder.USERS_WITH_DEFAULT_FOLDER,
				DctmDataType.DF_STRING.getStoredType());
			usersDefaultFolderPaths = new StoredProperty<IDfValue>(DctmFolder.USERS_DEFAULT_FOLDER_PATHS,
				DctmDataType.DF_STRING.getStoredType());
			while (resultCol.next()) {
				IDfValue v = resultCol.getValueAt(0);
				if (DctmMappingUtils.isMappableUser(ctx.getSession(), v.asString()) || ctx.isSpecialUser(v.asString())) {
					// We don't modify the home directory for mappable users or special users...
					continue;
				}

				usersWithDefaultFolder.addValue(DctmMappingUtils.substituteMappableUsers(folder, v));
				usersDefaultFolderPaths.addValue(resultCol.getValueAt(1));
			}
			properties.add(usersWithDefaultFolder);
			properties.add(usersDefaultFolderPaths);
		} finally {
			DfUtils.closeQuietly(resultCol);
		}
	}

	@Override
	protected Collection<IDfPersistentObject> findDependents(IDfSession session, StoredObject<IDfValue> marshaled,
		IDfFolder folder, DctmExportContext ctx) throws Exception {
		Collection<IDfPersistentObject> ret = super.findDependents(session, marshaled, folder, ctx);

		String owner = DctmMappingUtils.resolveMappableUser(session, folder.getOwnerName());
		if (!DctmMappingUtils.isSubstitutionForMappableUser(owner) && !ctx.isSpecialUser(owner)) {
			IDfUser user = session.getUser(owner);
			if (user != null) {
				ret.add(user);
			}
		}

		// Do the others
		IDfPersistentObject[] dep = {
			// The group
			session.getGroup(folder.getGroupName()),
			// The ACL
			folder.getACL(),
			// The type
			folder.getType()
		};

		for (IDfPersistentObject obj : dep) {
			if (obj == null) {
				continue;
			}
			ret.add(obj);
		}

		StoredProperty<IDfValue> usersWithDefaultFolder = marshaled.getProperty(DctmFolder.USERS_WITH_DEFAULT_FOLDER);
		if (usersWithDefaultFolder == null) { throw new Exception(String.format(
			"The export for folder [%s] does not contain the critical property [%s]", marshaled.getLabel(),
			DctmFolder.USERS_WITH_DEFAULT_FOLDER)); }
		for (IDfValue v : usersWithDefaultFolder) {
			IDfUser user = session.getUser(v.asString());
			if (user == null) {
				// in theory, this should be impossible as we just got the list via a direct query
				// to dm_user, and thus the users listed do exist
				throw new Exception(String.format(
					"Missing dependent for folder [%s] - user [%s] not found (as default folder)",
					marshaled.getLabel(), v.asString()));
			}
			ret.add(user);
		}

		// Export the object type
		// Save filestore name
		// String storageType = folder.getStorageType();
		// if (StringUtils.isNotBlank(storageType)) {
		// RepositoryConfiguration.getRepositoryConfiguration().addFileStore(storageType);
		// }
		return ret;
	}
}