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
import java.io.FileNotFoundException;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.sql.common.SqlCommon;
import com.armedia.caliente.engine.sql.common.SqlFile;
import com.armedia.caliente.engine.sql.common.SqlRoot;
import com.armedia.caliente.engine.sql.common.SqlSessionFactory;
import com.armedia.caliente.engine.sql.common.SqlSessionWrapper;
import com.armedia.caliente.engine.sql.common.SqlSetting;
import com.armedia.caliente.engine.sql.common.SqlTranslator;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.StreamTools;

public class SqlExportEngine extends
	ExportEngine<SqlRoot, SqlSessionWrapper, CmfValue, SqlExportContext, SqlExportContextFactory, SqlExportDelegateFactory, SqlExportEngineFactory> {

	public SqlExportEngine(SqlExportEngineFactory factory, Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings) {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings, false, SearchType.PATH);
	}

	@Override
	protected SearchType detectSearchType(String source) {
		if (source == null) { return null; }
		return SearchType.PATH;
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByQuery(SqlRoot session, CfgTools configuration, String query)
		throws Exception {
		throw new Exception("Local Export doesn't support queries");
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsBySearchKey(SqlRoot session, CfgTools configuration,
		String searchKey) throws Exception {
		throw new Exception("Local Export doesn't support ID-based searches");
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByPath(SqlRoot session, CfgTools configuration, String path)
		throws Exception {
		File f = session.getFile();
		if (!f.exists()) {
			throw new FileNotFoundException(String.format("Failed to find a file or folder at [%s]", f));
		}
		return StreamTools
			.of(new SqlRecursiveIterator(session, configuration.getBoolean(SqlSetting.IGNORE_EMPTY_FOLDERS)))
			.map((localFile) -> ExportTarget.from(
				localFile.getAbsolute().isFile() ? CmfObject.Archetype.DOCUMENT : CmfObject.Archetype.FOLDER,
				localFile.getId(), localFile.getSafePath()));
	}

	@Override
	protected String findFolderName(SqlRoot session, String folderId, Object ecmObject) {
		SqlFile sqlFile = SqlFile.class.cast(ecmObject);
		String path = sqlFile.getPortableFullPath();
		if (!sqlFile.isFolder()) {
			// Remove the last component of the path
			path = FileNameTools.dirname(path, '/');
		}

		while (path.length() > 1) {
			String id = SqlCommon.calculateId(path);
			if (StringUtils.equals(id, folderId)) { return FileNameTools.basename(path, '/'); }
			// Move up one level...
			path = FileNameTools.dirname(path, '/');
		}
		return null;
	}

	@Override
	protected CmfValue getValue(CmfValue.Type type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected CmfAttributeTranslator<CmfValue> getTranslator() {
		return new SqlTranslator();
	}

	@Override
	protected SqlSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new SqlSessionFactory(cfg, crypto);
	}

	@Override
	protected SqlExportContextFactory newContextFactory(SqlRoot session, CfgTools cfg, CmfObjectStore<?> objectStore,
		CmfContentStore<?, ?> streamStore, Transformer transformer, Logger output, WarningTracker warningTracker)
		throws Exception {
		return new SqlExportContextFactory(this, cfg, session, objectStore, streamStore, output, warningTracker);
	}

	@Override
	protected SqlExportDelegateFactory newDelegateFactory(SqlRoot session, CfgTools cfg) throws Exception {
		return new SqlExportDelegateFactory(this, cfg);
	}
}