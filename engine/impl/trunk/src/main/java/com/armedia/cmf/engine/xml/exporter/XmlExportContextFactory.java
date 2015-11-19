package com.armedia.cmf.engine.xml.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContextFactory;
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.engine.xml.common.XmlSessionWrapper;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class XmlExportContextFactory extends
	ExportContextFactory<XmlRoot, XmlSessionWrapper, CmfValue, XmlExportContext, XmlExportEngine> {

	protected XmlExportContextFactory(XmlExportEngine engine, CfgTools settings, XmlRoot session)
		throws Exception {
		super(engine, settings, session);
	}

	@Override
	protected XmlExportContext constructContext(String rootId, CmfType rootType, XmlRoot session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?> contentStore, CmfTypeMapper typeMapper) {
		return new XmlExportContext(this, getSettings(), rootId, rootType, session, output);
	}

	@Override
	protected String calculateProductName(XmlRoot session) throws Exception {
		return "LocalFilesystem";
	}

	@Override
	protected String calculateProductVersion(XmlRoot session) throws Exception {
		return "1.0";
	}
}