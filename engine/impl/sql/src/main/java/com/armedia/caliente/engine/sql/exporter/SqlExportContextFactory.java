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

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportContextFactory;
import com.armedia.caliente.engine.sql.common.SqlRoot;
import com.armedia.caliente.engine.sql.common.SqlSessionWrapper;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class SqlExportContextFactory
	extends ExportContextFactory<SqlRoot, SqlSessionWrapper, CmfValue, SqlExportContext, SqlExportEngine> {

	protected SqlExportContextFactory(SqlExportEngine engine, CfgTools settings, SqlRoot session,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, Logger output, WarningTracker warningTracker)
		throws Exception {
		super(engine, settings, session, objectStore, contentStore, output, warningTracker);
	}

	@Override
	protected SqlExportContext constructContext(String rootId, CmfObject.Archetype rootType, SqlRoot session,
		int historyPosition) {
		return new SqlExportContext(this, getSettings(), rootId, rootType, session, getOutput(), getWarningTracker());
	}

	@Override
	protected String calculateProductName(SqlRoot session) throws Exception {
		return "LocalFilesystem";
	}

	@Override
	protected String calculateProductVersion(SqlRoot session) throws Exception {
		return "1.0";
	}
}