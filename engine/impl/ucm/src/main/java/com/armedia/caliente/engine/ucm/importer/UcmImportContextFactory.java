package com.armedia.caliente.engine.ucm.importer;

import org.apache.chemistry.opencmis.client.api.Folder;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.importer.ImportContextFactory;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class UcmImportContextFactory
	extends ImportContextFactory<UcmSession, UcmSessionWrapper, CmfValue, UcmImportContext, UcmImportEngine, Folder> {

	UcmImportContextFactory(UcmImportEngine engine, UcmSession session, CfgTools settings,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CmfTypeMapper typeMapper,
		Logger output, WarningTracker warningTracker) throws Exception {
		super(engine, settings, session, objectStore, contentStore, typeMapper, output, warningTracker);
	}

	@Override
	protected UcmImportContext constructContext(String rootId, CmfType rootType, UcmSession session,
		int historyPosition) {
		return new UcmImportContext(this, rootId, rootType, session, getOutput(), getWarningTracker(), getTypeMapper(),
			getEngine().getTranslator(), getObjectStore(), getContentStore(), historyPosition);
	}

	@Override
	protected Folder locateFolder(UcmSession session, String path) throws Exception {
		return null;
	}

	@Override
	protected Folder createFolder(UcmSession session, Folder parent, String name) throws Exception {
		return null;
	}

	@Override
	public final String calculateProductName(UcmSession session) {
		return null;
	}

	@Override
	public final String calculateProductVersion(UcmSession session) {
		return null;
	}
}