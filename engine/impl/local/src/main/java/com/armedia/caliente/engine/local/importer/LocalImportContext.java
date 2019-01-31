package com.armedia.caliente.engine.local.importer;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportContext;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportContext extends ImportContext<LocalRoot, CmfValue, LocalImportContextFactory> {

	public LocalImportContext(LocalImportContextFactory factory, CfgTools settings, String rootId,
		CmfObject.Archetype rootType, LocalRoot session, Logger output, WarningTracker warningTracker,
		Transformer transformer, CmfAttributeTranslator<CmfValue> translator, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> streamStore, int batchPosition) {
		super(factory, settings, rootId, rootType, session, output, warningTracker, transformer, translator,
			objectStore, streamStore, batchPosition);
	}
}