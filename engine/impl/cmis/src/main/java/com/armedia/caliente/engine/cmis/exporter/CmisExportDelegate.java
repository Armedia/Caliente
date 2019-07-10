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
package com.armedia.caliente.engine.cmis.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.caliente.engine.cmis.CmisSessionWrapper;
import com.armedia.caliente.engine.exporter.ExportDelegate;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public abstract class CmisExportDelegate<T> extends
	ExportDelegate<T, Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportDelegateFactory, CmisExportEngine> {

	protected CmisExportDelegate(CmisExportDelegateFactory factory, Session session, Class<T> objectClass, T object)
		throws Exception {
		super(factory, session, objectClass, object);
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyAntecedents(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifySuccessors(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyDependents(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected List<CmfContentStream> storeContent(CmisExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?> streamStore,
		boolean includeRenditions) {
		return new ArrayList<>();
	}
}