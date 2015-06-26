package com.armedia.cmf.engine.local.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContextFactory;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportContextFactory extends
	ExportContextFactory<LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportEngine> {

	protected LocalExportContextFactory(LocalExportEngine engine, CfgTools settings, LocalRoot session)
		throws Exception {
		super(engine, settings, session);
	}

	@Override
	protected LocalExportContext constructContext(String rootId, CmfType rootType, LocalRoot session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?> contentStore, CmfTypeMapper typeMapper) {
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