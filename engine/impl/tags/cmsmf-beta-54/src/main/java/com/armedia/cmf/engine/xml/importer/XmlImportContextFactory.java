package com.armedia.cmf.engine.xml.importer;

import java.io.File;

import org.slf4j.Logger;

import com.armedia.cmf.engine.importer.ImportContextFactory;
import com.armedia.cmf.engine.xml.common.XmlRoot;
import com.armedia.cmf.engine.xml.common.XmlSessionWrapper;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class XmlImportContextFactory
	extends ImportContextFactory<XmlRoot, XmlSessionWrapper, CmfValue, XmlImportContext, XmlImportEngine, File> {

	protected XmlImportContextFactory(XmlImportEngine engine, CfgTools settings, XmlRoot root) throws Exception {
		super(engine, settings, root);
	}

	@Override
	protected File locateFolder(XmlRoot session, String path) throws Exception {
		File f = new File(session.getFile(), path).getCanonicalFile();
		if (f.exists() && f.isDirectory()) { return f; }
		return null;
	}

	@Override
	protected File createFolder(XmlRoot session, File parent, String name) throws Exception {
		File f = new File(parent, name);
		f.mkdirs();
		if (!f.exists()) { throw new Exception(String.format("Could not create the directory at [%s]", name)); }
		if (!f.isDirectory()) { throw new Exception(String.format("A non-directory already exists at [%s]", name)); }
		return f;
	}

	@Override
	protected XmlImportContext constructContext(String rootId, CmfType rootType, XmlRoot session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CmfTypeMapper typeMapper,
		int batchPosition) {
		return new XmlImportContext(this, getSettings(), rootId, rootType, session, output, typeMapper,
			getEngine().getTranslator(), objectStore, contentStore, batchPosition);
	}

	@Override
	protected String calculateProductName(XmlRoot session) throws Exception {
		return "XmlMetadata";
	}

	@Override
	protected String calculateProductVersion(XmlRoot session) throws Exception {
		return "1.0";
	}
}