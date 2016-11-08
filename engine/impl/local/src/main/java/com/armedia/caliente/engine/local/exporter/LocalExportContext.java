package com.armedia.caliente.engine.local.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.exporter.ExportContext;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportContext extends ExportContext<LocalRoot, CmfValue, LocalExportContextFactory> {

	public LocalExportContext(LocalExportContextFactory factory, CfgTools settings, String rootId, CmfType rootType,
		LocalRoot session, Logger output) {
		super(factory, settings, rootId, rootType, session, output);
	}
}