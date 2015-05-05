package com.armedia.cmf.engine.local.exporter;

import java.io.File;

import com.armedia.commons.utilities.CfgTools;

public class LocalDocumentExportDelegate extends LocalExportDelegate {

	protected LocalDocumentExportDelegate(LocalExportEngine engine, File object, CfgTools configuration)
		throws Exception {
		super(engine, object, configuration);
	}
}