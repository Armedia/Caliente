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
package com.armedia.caliente.engine.cmis.importer;

import java.util.Collection;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportContext;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;

public class CmisImportContext extends ImportContext<Session, CmfValue, CmisImportContextFactory> {

	CmisImportContext(CmisImportContextFactory factory, String rootId, CmfObject.Archetype rootType, Session session,
		Logger output, WarningTracker warningTracker, Transformer transformer,
		CmfAttributeTranslator<CmfValue> translator, CmfObjectStore<?> objectStore, CmfContentStore<?, ?> streamStore,
		int batchPosition) {
		super(factory, factory.getSettings(), rootId, rootType, session, output, warningTracker, transformer,
			translator, objectStore, streamStore, batchPosition);
	}

	public Set<String> convertAllowableActionsToPermissions(Collection<String> allowableActions) {
		return getFactory().convertAllowableActionsToPermissions(allowableActions);
	}

	public final RepositoryInfo getRepositoryInfo() {
		return getFactory().getRepositoryInfo();
	}
}