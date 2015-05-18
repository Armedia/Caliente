package com.armedia.cmf.engine.local.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.importer.ImportContext;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportContext extends ImportContext<LocalRoot, CmfValue> {

	public LocalImportContext(LocalImportContextFactory factory, CfgTools settings, String rootId,
		CmfType rootType, LocalRoot session, Logger output, CmfAttributeTranslator<CmfValue> translator,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?> streamStore) {
		super(factory, settings, rootId, rootType, session, output, translator, objectStore, streamStore);
	}
}