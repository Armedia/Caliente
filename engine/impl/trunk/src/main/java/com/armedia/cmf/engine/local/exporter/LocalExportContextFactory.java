package com.armedia.cmf.engine.local.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContextFactory;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportContextFactory extends
ExportContextFactory<LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportEngine> {

	protected LocalExportContextFactory(LocalExportEngine engine, CfgTools settings) {
		super(engine, settings);
	}

	@Override
	protected LocalExportContext constructContext(String rootId, CmfType rootType, LocalRoot session,
		Logger output, CmfObjectStore<?, ?> objectStore, CmfContentStore<?> contentStore) {
		return new LocalExportContext(this, getSettings(), rootId, rootType, session, output);
	}
}