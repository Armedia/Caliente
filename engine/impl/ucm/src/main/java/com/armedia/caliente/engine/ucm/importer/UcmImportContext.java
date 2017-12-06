package com.armedia.caliente.engine.ucm.importer;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportContext;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;

public class UcmImportContext extends ImportContext<UcmSession, CmfValue, UcmImportContextFactory> {

	UcmImportContext(UcmImportContextFactory factory, String rootId, CmfType rootType, UcmSession session,
		Logger output, WarningTracker warningTracker, Transformer transformer,
		CmfAttributeTranslator<CmfValue> translator, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> streamStore, int batchPosition) {
		super(factory, factory.getSettings(), rootId, rootType, session, output, warningTracker, transformer,
			translator, objectStore, streamStore, batchPosition);
	}
}