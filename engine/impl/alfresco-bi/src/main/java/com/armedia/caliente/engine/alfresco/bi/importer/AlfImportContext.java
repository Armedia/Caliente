package com.armedia.caliente.engine.alfresco.bi.importer;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.alfresco.bi.AlfRoot;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportContext;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class AlfImportContext extends ImportContext<AlfRoot, CmfValue, AlfImportContextFactory> {

	public AlfImportContext(AlfImportContextFactory factory, CfgTools settings, String rootId,
		CmfObject.Archetype rootType, AlfRoot session, Logger output, WarningTracker tracker, Transformer transformer,
		CmfAttributeTranslator<CmfValue> translator, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> streamStore, int batchPosition) {
		super(factory, settings, rootId, rootType, session, output, tracker, transformer, translator, objectStore,
			streamStore, batchPosition);
	}

	public final String getAlternateName(CmfObject.Archetype type, String id) throws ImportException {
		return getFactory().getAlternateName(type, id);
	}

	public final Map<CmfObjectRef, String> getObjectNames(Collection<CmfObjectRef> refs, boolean current)
		throws ImportException {
		return getFactory().getObjectNames(refs, current);
	}

	protected String getObjectName(CmfObject<CmfValue> object) {
		return getObjectName(object, true);
	}

	protected String getObjectName(CmfObject<CmfValue> object, boolean current) {
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