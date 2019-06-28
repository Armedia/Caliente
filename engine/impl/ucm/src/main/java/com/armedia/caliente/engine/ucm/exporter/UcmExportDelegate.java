/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.engine.ucm.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.caliente.engine.exporter.ExportDelegate;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.engine.ucm.model.UcmModelObject;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public abstract class UcmExportDelegate<T extends UcmModelObject> extends
	ExportDelegate<T, UcmSession, UcmSessionWrapper, CmfValue, UcmExportContext, UcmExportDelegateFactory, UcmExportEngine> {

	protected UcmExportDelegate(UcmExportDelegateFactory factory, UcmSession session, Class<T> objectClass, T object)
		throws Exception {
		super(factory, session, objectClass, object);
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		UcmExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifyAntecedents(CmfObject<CmfValue> marshalled, UcmExportContext ctx)
		throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifySuccessors(CmfObject<CmfValue> marshalled, UcmExportContext ctx)
		throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifyDependents(CmfObject<CmfValue> marshalled, UcmExportContext ctx)
		throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected List<CmfContentStream> storeContent(UcmExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?> streamStore,
		boolean includeRenditions) {
		return new ArrayList<>();
	}
}