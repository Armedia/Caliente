package com.armedia.caliente.engine.ucm.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportContextFactory;
import com.armedia.caliente.engine.ucm.IdcSession;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class UcmExportContextFactory
	extends ExportContextFactory<IdcSession, UcmSessionWrapper, CmfValue, UcmExportContext, UcmExportEngine> {

	UcmExportContextFactory(UcmExportEngine engine, IdcSession session, CfgTools settings,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Logger output,
		WarningTracker warningTracker) throws Exception {
		super(engine, settings, session, objectStore, contentStore, output, warningTracker);
	}

	@Override
	protected UcmExportContext constructContext(String rootId, CmfType rootType, IdcSession session,
		int batchPosition) {
		return new UcmExportContext(this, rootId, rootType, session, getOutput(), getWarningTracker());
	}

	@Override
	public final String calculateProductName(IdcSession session) {
		return null;
	}

	@Override
	public final String calculateProductVersion(IdcSession session) {
		return null;
	}
}