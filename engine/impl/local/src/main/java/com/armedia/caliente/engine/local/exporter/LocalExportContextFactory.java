package com.armedia.caliente.engine.local.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportContextFactory;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfArchetype;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportContextFactory
	extends ExportContextFactory<LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportEngine> {

	protected LocalExportContextFactory(LocalExportEngine engine, CfgTools settings, LocalRoot session,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Logger output,
		WarningTracker warningTracker) throws Exception {
		super(engine, settings, session, objectStore, contentStore, output, warningTracker);
	}

	@Override
	protected LocalExportContext constructContext(String rootId, CmfArchetype rootType, LocalRoot session,
		int historyPosition) {
		return new LocalExportContext(this, getSettings(), rootId, rootType, session, getOutput(), getWarningTracker());
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