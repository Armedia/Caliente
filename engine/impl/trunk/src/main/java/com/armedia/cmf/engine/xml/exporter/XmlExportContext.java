package com.armedia.cmf.engine.xml.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class XmlExportContext extends ExportContext<XmlRoot, CmfValue, XmlExportContextFactory> {

	public XmlExportContext(XmlExportContextFactory factory, CfgTools settings, String rootId, CmfType rootType,
		XmlRoot session, Logger output) {
		super(factory, settings, rootId, rootType, session, output);
	}
}