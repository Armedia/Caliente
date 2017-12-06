package com.armedia.caliente.engine.ucm.importer;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportContextFactory;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class UcmImportContextFactory
	extends ImportContextFactory<UcmSession, UcmSessionWrapper, CmfValue, UcmImportContext, UcmImportEngine, Object> {

	UcmImportContextFactory(UcmImportEngine engine, UcmSession session, CfgTools settings,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
		super(engine, settings, session, objectStore, contentStore, transformer, output, warningTracker);
	}

	@Override
	protected UcmImportContext constructContext(String rootId, CmfType rootType, UcmSession session,
		int historyPosition) {
		return new UcmImportContext(this, rootId, rootType, session, getOutput(), getWarningTracker(), getTransformer(),
			getEngine().getTranslator(), getObjectStore(), getContentStore(), historyPosition);
	}

	@Override
	protected Object locateFolder(UcmSession session, String path) throws Exception {
		return null;
	}

	@Override
	protected Object createFolder(UcmSession session, Object parent, String name) throws Exception {
		return null;
	}

	@Override
	public final String calculateProductName(UcmSession session) {
		return "Oracle";
	}

	@Override
	public final String calculateProductVersion(UcmSession session) {
		return "WebCenter 11g";
	}
}