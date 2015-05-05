package com.armedia.cmf.engine.local.exporter;

import java.io.File;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportContext extends ExportContext<File, StoredValue> {

	public LocalExportContext(LocalExportContextFactory factory, CfgTools settings, String rootId,
		StoredObjectType rootType, File session, Logger output) {
		super(factory, settings, rootId, rootType, session, output);
	}
}