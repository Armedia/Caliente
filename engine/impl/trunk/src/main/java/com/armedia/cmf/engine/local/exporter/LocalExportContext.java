package com.armedia.cmf.engine.local.exporter;

import java.net.URL;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportContext extends ExportContext<URL, StoredValue> {

	public LocalExportContext(LocalExportContextFactory factory, CfgTools settings, String rootId,
		StoredObjectType rootType, URL session, Logger output) {
		super(factory, settings, rootId, rootType, session, output);
	}
}