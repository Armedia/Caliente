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
package com.armedia.caliente.engine.alfresco.bi.importer;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.alfresco.bi.AlfRoot;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportContext;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class AlfImportContext extends ImportContext<AlfRoot, CmfValue, AlfImportContextFactory> {

	public AlfImportContext(AlfImportContextFactory factory, CfgTools settings, String rootId,
		CmfObject.Archetype rootType, AlfRoot session, Logger output, WarningTracker tracker, Transformer transformer,
		CmfAttributeTranslator<CmfValue> translator, CmfObjectStore<?> objectStore, CmfContentStore<?, ?> streamStore,
		int batchPosition) {
		super(factory, settings, rootId, rootType, session, output, tracker, transformer, translator, objectStore,
			streamStore, batchPosition);
	}

	public final Map<CmfObjectRef, String> getObjectNames(Collection<CmfObjectRef> refs, boolean current)
		throws ImportException {
		return getFactory().getObjectNames(refs, current);
	}

	protected String getObjectName(CmfObject<CmfValue> object) {
		return getObjectName(object, true);
	}

	protected String getObjectName(CmfObject<CmfValue> object, boolean current) {
		CmfProperty<CmfValue> prop = object.getProperty(IntermediateProperty.HEAD_NAME);
		if ((prop != null) && prop.hasValues()) { return prop.getValue().toString(); }

		// No explicitly-set HEAD name, find the name for the HEAD revision
		CmfObject<CmfValue> head = object;
		if (current) {
			try {
				head = getHeadObject(object);
			} catch (CmfStorageException e) {
				this.log.warn("Failed to load the HEAD object for {} history [{}]", object.getType().name(),
					object.getHistoryId(), e);
			}
			if (head == null) {
				head = object;
			}
		}
		return getFactory().getEngine().getObjectName(head);
	}
}