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
package com.armedia.caliente.engine.sql.exporter;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.attribute.UserPrincipalLookupService;

import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.sql.common.SqlCommon;
import com.armedia.caliente.engine.sql.common.SqlFile;
import com.armedia.caliente.engine.sql.common.SqlRoot;
import com.armedia.caliente.engine.sql.common.SqlSessionWrapper;
import com.armedia.caliente.engine.sql.common.SqlSetting;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class SqlExportDelegateFactory
	extends ExportDelegateFactory<SqlRoot, SqlSessionWrapper, CmfValue, SqlExportContext, SqlExportEngine> {

	private final SqlRoot root;
	private final boolean copyContent;
	private final UserPrincipalLookupService userDb;

	protected SqlExportDelegateFactory(SqlExportEngine engine, CfgTools configuration) throws Exception {
		super(engine, configuration);
		File root = SqlCommon.getRootDirectory(configuration);
		if (root == null) { throw new IllegalArgumentException("Must provide a root directory to work from"); }
		this.root = new SqlRoot(root);
		this.copyContent = configuration.getBoolean(SqlSetting.COPY_CONTENT);
		this.userDb = FileSystems.getDefault().getUserPrincipalLookupService();
	}

	public final SqlRoot getRoot() {
		return this.root;
	}

	public final boolean isCopyContent() {
		return this.copyContent;
	}

	@Override
	protected SqlExportDelegate<?> newExportDelegate(SqlRoot session, CmfObject.Archetype type, String searchKey)
		throws Exception {
		switch (type) {
			case FOLDER:
			case DOCUMENT:
				return new SqlFileExportDelegate(this, session, SqlFile.newFromSafePath(session, searchKey));
			case USER:
				return new SqlPrincipalExportDelegate(this, session, this.userDb.lookupPrincipalByName(searchKey));
			case GROUP:
				return new SqlPrincipalExportDelegate(this, session,
					this.userDb.lookupPrincipalByGroupName(searchKey));
			default:
				break;
		}
		this.log.warn("Type [{}] is not supported - no delegate created for search key [{}]", type, searchKey);
		return null;
	}
}