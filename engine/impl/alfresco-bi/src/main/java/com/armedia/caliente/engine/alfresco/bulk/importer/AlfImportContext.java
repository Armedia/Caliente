package com.armedia.caliente.engine.alfresco.bulk.importer;

import org.slf4j.Logger;

import com.armedia.caliente.engine.alfresco.bulk.common.AlfRoot;
import com.armedia.caliente.engine.importer.ImportContext;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class AlfImportContext extends ImportContext<AlfRoot, CmfValue, AlfImportContextFactory> {

	public AlfImportContext(AlfImportContextFactory factory, CfgTools settings, String rootId, CmfType rootType,
		AlfRoot session, Logger output, CmfTypeMapper typeMapper, CmfAttributeTranslator<CmfValue> translator,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, int batchPosition) {
		super(factory, settings, rootId, rootType, session, output, typeMapper, translator, objectStore, streamStore,
			batchPosition);
	}

	public final String getAlternateName(CmfType type, String id) {
		return this.getFactory().getAlternateName(type, id);
	}
}