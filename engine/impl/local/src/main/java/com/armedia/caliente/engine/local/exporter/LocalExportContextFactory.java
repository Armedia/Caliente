package com.armedia.caliente.engine.local.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.exporter.ExportContextFactory;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportContextFactory
	extends ExportContextFactory<LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportEngine> {

	protected LocalExportContextFactory(LocalExportEngine engine, CfgTools settings, LocalRoot session,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Logger output) throws Exception {
		super(engine, settings, session, objectStore, contentStore, output);
	}

	@Override
	protected LocalExportContext constructContext(String rootId, CmfType rootType, LocalRoot session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CmfTypeMapper typeMapper,
		int batchPosition) {
		return new LocalExportContext(this, getSettings(), rootId, rootType, session, output);
	}

	@Override
	protected String calculateProductName(LocalRoot session) throws Exception {
		return "LocalFilesystem";
	}

	@Override
	protected String calculateProductVersion(LocalRoot session) throws Exception {
		return "1.0";
	}
}