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
package com.armedia.caliente.engine.sql.importer;

import java.io.IOException;

import com.armedia.caliente.engine.importer.ImportDelegateFactory;
import com.armedia.caliente.engine.sql.common.SqlRoot;
import com.armedia.caliente.engine.sql.common.SqlSessionWrapper;
import com.armedia.caliente.engine.sql.common.SqlSetting;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class SqlImportDelegateFactory
	extends ImportDelegateFactory<SqlRoot, SqlSessionWrapper, CmfValue, SqlImportContext, SqlImportEngine> {

	private final boolean includeAllVersions;
	private final boolean includeAllStreams;
	private final boolean includeMetadata;
	private final boolean failOnCollisions;

	public SqlImportDelegateFactory(SqlImportEngine engine, CfgTools configuration) throws IOException {
		super(engine, configuration);
		this.includeAllVersions = configuration.getBoolean(SqlSetting.INCLUDE_ALL_VERSIONS);
		this.includeAllStreams = configuration.getBoolean(SqlSetting.INCLUDE_ALL_STREAMS);
		this.includeMetadata = configuration.getBoolean(SqlSetting.INCLUDE_METADATA);
		this.failOnCollisions = configuration.getBoolean(SqlSetting.FAIL_ON_COLLISIONS);
	}

	public final boolean isIncludeMetadata() {
		return this.includeMetadata;
	}

	public final boolean isIncludeAllStreams() {
		return this.includeAllStreams;
	}

	public final boolean isIncludeAllVersions() {
		return this.includeAllVersions;
	}

	public final boolean isFailOnCollisions() {
		return this.failOnCollisions;
	}

	@Override
	protected SqlImportDelegate newImportDelegate(CmfObject<CmfValue> storedObject) throws Exception {
		switch (storedObject.getType()) {
			case DOCUMENT:
				return new SqlDocumentImportDelegate(this, storedObject);
			case FOLDER:
				return new SqlFolderImportDelegate(this, storedObject);
			default:
				return null;
		}
	}
}