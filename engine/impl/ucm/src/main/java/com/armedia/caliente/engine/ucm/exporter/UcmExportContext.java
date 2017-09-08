package com.armedia.caliente.engine.ucm.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportContext;
import com.armedia.caliente.engine.ucm.IdcSession;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;

public class UcmExportContext extends ExportContext<IdcSession, CmfValue, UcmExportContextFactory> {

	UcmExportContext(UcmExportContextFactory factory, String rootId, CmfType rootType, IdcSession session,
		Logger output, WarningTracker warningTracker) {
		super(factory, factory.getSettings(), rootId, rootType, session, output, warningTracker);
	}

}