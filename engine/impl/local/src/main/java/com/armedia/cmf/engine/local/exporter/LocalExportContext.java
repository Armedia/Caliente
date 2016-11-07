package com.armedia.cmf.engine.local.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportContext extends ExportContext<LocalRoot, CmfValue, LocalExportContextFactory> {

	public LocalExportContext(LocalExportContextFactory factory, CfgTools settings, String rootId, CmfType rootType,
		LocalRoot session, Logger output) {
		super(factory, settings, rootId, rootType, session, output);
	}
}