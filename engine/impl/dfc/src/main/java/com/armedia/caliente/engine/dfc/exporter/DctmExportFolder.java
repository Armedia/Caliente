/**
 *
 */

package com.armedia.caliente.engine.dfc.exporter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmDataType;
import com.armedia.caliente.engine.dfc.DctmMappingUtils;
import com.armedia.caliente.engine.dfc.common.DctmFolder;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.commons.dfc.util.DfUtils;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
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

	/**
	 * This DQL will find all groups for which this folder is marked as the default folder, and thus
	 * all groups for whom it must be restored later on.
	 */
	private static final String DQL_FIND_GROUPS_WITH_DEFAULT_FOLDER = "SELECT g.group_name FROM dm_group g WHERE g.group_directory_id = '%s'";

	protected DctmExportFolder(DctmExportDelegateFactory factory, IDfSession session, IDfFolder folder)
		throws Exception {
		super(factory, session, IDfFolder.class, folder);
	}

	DctmExportFolder(DctmExportDelegateFactory factory, IDfSession session, IDfPersistentObject folder)
		throws Exception {
		this(factory, session, DctmExportDelegate.staticCast(IDfFolder.class, folder));
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfFolder folder) throws Exception {
		return folder.getFolderPath(0);
	}

	private int calculateFolderDepth(IDfSession session, IDfId folderId, Set<String> visited) throws DfException {
		if (visited == null) {
			visited = new LinkedHashSet<>();
		}
		// If the folder has already been visited, we have a loop...so let's explode loudly
		if (!visited.add(folderId.getId())) { throw new DfException(String
			.format("Folder loop detected, element [%s] exists twice: %s", folderId.getId(), visited.toString())); }
		try {
			int depth = 0;
			String dql = String.format("select distinct i_folder_id from dm_sysobject where r_object_id = '%s'",
				folderId.getId());
			IDfCollection results = DfUtils.executeQuery(session, dql, IDfQuery.DF_EXECREAD_QUERY);
			try {
				while (results.next()) {
					// My depth is the maximum depth from any of my parents, plus one
					IDfId parentId = results.getId(DctmAttributes.I_FOLDER_ID);
					if (parentId.isNull() || !parentId.isObjectId()) {
						continue;
					}
					depth = Math.max(depth, calculateFolderDepth(session, parentId, visited) + 1);
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
	protected int calculateDepth(IDfFolder folder, Set<String> visited) throws DfException {
		// Calculate the maximum depth that this folder resides in, from its parents.
		// Keep track of visited nodes, and explode on a loop.
		return calculateFolderDepth(folder.getSession(), folder.getObjectId(), visited);
	}

	@Override
	protected boolean getDataProperties(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties,
		IDfFolder folder) throws DfException, ExportException {
		super.getDataProperties(ctx, properties, folder);
		final String folderId = folder.getObjectId().getId();

		IDfCollection resultCol = DfUtils.executeQuery(folder.getSession(),
			String.format(DctmExportFolder.DQL_FIND_USERS_WITH_DEFAULT_FOLDER, folderId), IDfQuery.DF_EXECREAD_QUERY);
		try {
			CmfProperty<IDfValue> usersWithDefaultFolder = new CmfProperty<>(
				IntermediateProperty.USERS_WITH_DEFAULT_FOLDER, DctmDataType.DF_STRING.getStoredType());
			CmfProperty<IDfValue> usersDefaultFolderPaths = new CmfProperty<>(
				IntermediateProperty.USERS_DEFAULT_FOLDER_PATHS, DctmDataType.DF_STRING.getStoredType());
			while (resultCol.next()) {
				IDfValue v = resultCol.getValueAt(0);
				if (DctmMappingUtils.isMappableUser(ctx.getSession(), v.asString())
					|| ctx.isSpecialUser(v.asString())) {
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

		resultCol = DfUtils.executeQuery(folder.getSession(),
			String.format(DctmExportFolder.DQL_FIND_GROUPS_WITH_DEFAULT_FOLDER, folderId), IDfQuery.DF_EXECREAD_QUERY);
		try {
			CmfProperty<IDfValue> groupsWithDefaultFolder = new CmfProperty<>(
				IntermediateProperty.GROUPS_WITH_DEFAULT_FOLDER, DctmDataType.DF_STRING.getStoredType());
			while (resultCol.next()) {
				IDfValue v = resultCol.getValueAt(0);
				if (ctx.isSpecialGroup(v.asString())) {
					// We don't modify the home directory for special groups...
					continue;
				}

				groupsWithDefaultFolder.addValue(v);
			}
			properties.add(groupsWithDefaultFolder);
		} finally {
			DfUtils.closeQuietly(resultCol);
		}
		return true;
	}
}