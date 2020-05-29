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

import java.util.List;

import com.armedia.caliente.engine.exporter.ExportDelegate;
import com.armedia.caliente.engine.sql.common.SqlRoot;
import com.armedia.caliente.engine.sql.common.SqlSessionWrapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

abstract class SqlExportDelegate<T> extends
	ExportDelegate<T, SqlRoot, SqlSessionWrapper, CmfValue, SqlExportContext, SqlExportDelegateFactory, SqlExportEngine> {

	protected final SqlRoot root;

	protected SqlExportDelegate(SqlExportDelegateFactory factory, SqlRoot root, Class<T> klass, T object)
		throws Exception {
		super(factory, root, klass, object);
		this.root = root;
	}

	@Override
	protected boolean calculateHistoryCurrent(SqlRoot root, T object) throws Exception {
		// Always true
		return true;
	}

	@Override
	protected List<CmfContentStream> storeContent(SqlExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, CmfContentStore<?, ?> streamStore, boolean includeRenditions) {
		return null;
	}
}