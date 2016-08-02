package com.armedia.cmf.engine.alfresco.bulk.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.alfresco.bulk.common.AlfRoot;
import com.armedia.cmf.engine.importer.ImportContext;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class AlfImportContext extends ImportContext<AlfRoot, CmfValue, AlfImportContextFactory> {

	public AlfImportContext(AlfImportContextFactory factory, CfgTools settings, String rootId, CmfType rootType,
		AlfRoot session, Logger output, CmfTypeMapper typeMapper, CmfAttributeTranslator<CmfValue> translator,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, int batchPosition) {
		super(factory, settings, rootId, rootType, session, output, typeMapper, translator, objectStore, streamStore,
			batchPosition);
	}
}