/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
/**
 *
 */

package com.armedia.caliente.engine.dfc.exporter;

import java.util.Collection;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dfc.DctmDataType;
import com.armedia.caliente.engine.dfc.DctmMappingUtils;
import com.armedia.caliente.engine.dfc.common.DctmFolder;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.tools.dfc.DfcQuery;
import com.armedia.commons.utilities.FileNameTools;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 *
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

	@Override
	protected int calculateDepth(IDfFolder folder) throws DfException {
		// Calculate the maximum depth that this folder resides in, from its parents.
		// Keep track of visited nodes, and explode on a loop.
		final int pathCount = folder.getFolderPathCount();
		int max = -1;
		for (int i = 0; i < pathCount; i++) {
			String path = folder.getFolderPath(i);
			max = Math.max(max, FileNameTools.tokenize(path, '/').size());
		}
		if (max < 0) {
			throw new DfException(String.format("Failed to calculate the depth for %s [%s](%s)",
				this.exportTarget.getType(), this.label, this.exportTarget.getId()));
		}
		return max;
	}

	@Override
	protected boolean getDataProperties(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties,
		IDfFolder folder) throws DfException, ExportException {
		super.getDataProperties(ctx, properties, folder);
		final String folderId = folder.getObjectId().getId();

		try (DfcQuery query = new DfcQuery(ctx.getSession(),
			String.format(DctmExportFolder.DQL_FIND_USERS_WITH_DEFAULT_FOLDER, folderId),
			DfcQuery.Type.DF_EXECREAD_QUERY)) {
			CmfProperty<IDfValue> usersWithDefaultFolder = new CmfProperty<>(
				IntermediateProperty.USERS_WITH_DEFAULT_FOLDER, DctmDataType.DF_STRING.getStoredType());
			CmfProperty<IDfValue> usersDefaultFolderPaths = new CmfProperty<>(
				IntermediateProperty.USERS_DEFAULT_FOLDER_PATHS, DctmDataType.DF_STRING.getStoredType());
			query.forEachRemaining((resultCol) -> {
				IDfValue v = resultCol.getValueAt(0);
				if (DctmMappingUtils.isMappableUser(ctx.getSession(), v.asString())
					|| ctx.isSpecialUser(v.asString())) {
					// We don't modify the home directory for mappable users or special users...
					return;
				}

				usersWithDefaultFolder.addValue(DctmMappingUtils.substituteMappableUsers(folder, v));
				usersDefaultFolderPaths.addValue(resultCol.getValueAt(1));
			});
			properties.add(usersWithDefaultFolder);
			properties.add(usersDefaultFolderPaths);
		}

		try (DfcQuery query = new DfcQuery(ctx.getSession(),
			String.format(DctmExportFolder.DQL_FIND_GROUPS_WITH_DEFAULT_FOLDER, folderId),
			DfcQuery.Type.DF_EXECREAD_QUERY)) {
			CmfProperty<IDfValue> groupsWithDefaultFolder = new CmfProperty<>(
				IntermediateProperty.GROUPS_WITH_DEFAULT_FOLDER, DctmDataType.DF_STRING.getStoredType());
			query.forEachRemaining((resultCol) -> {
				IDfValue v = resultCol.getValueAt(0);
				if (ctx.isSpecialGroup(v.asString())) {
					// We don't modify the home directory for special groups...
					return;
				}

				groupsWithDefaultFolder.addValue(v);
			});
			properties.add(groupsWithDefaultFolder);
		}
		return true;
	}
}