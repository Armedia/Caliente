package com.armedia.caliente.engine.local.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportContext;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportContext extends ExportContext<LocalRoot, CmfValue, LocalExportContextFactory> {

	public LocalExportContext(LocalExportContextFactory factory, CfgTools settings, String rootId, CmfObject.Archetype rootType,
		LocalRoot session, Logger output, WarningTracker warningTracker) {
		super(factory, settings, rootId, rootType, session, output, warningTracker);
	}
}