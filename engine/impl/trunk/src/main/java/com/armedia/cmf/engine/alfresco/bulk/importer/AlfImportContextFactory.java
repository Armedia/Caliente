package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.File;

import org.slf4j.Logger;

import com.armedia.cmf.engine.alfresco.bulk.common.AlfRoot;
import com.armedia.cmf.engine.alfresco.bulk.common.AlfSessionWrapper;
import com.armedia.cmf.engine.importer.ImportContextFactory;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class AlfImportContextFactory extends
ImportContextFactory<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportEngine, File> {

	protected AlfImportContextFactory(AlfImportEngine engine, CfgTools settings, AlfRoot root) throws Exception {
		super(engine, settings, root);
	}

	@Override
	protected File locateFolder(AlfRoot session, String path) throws Exception {
		File f = new File(session.getFile(), path).getCanonicalFile();
		if (f.exists() && f.isDirectory()) { return f; }
		return null;
	}

	@Override
	protected File createFolder(AlfRoot session, File parent, String name) throws Exception {
		File f = new File(parent, name);
		f.mkdirs();
		if (!f.exists()) { throw new Exception(String.format("Could not create the directory at [%s]", name)); }
		if (!f.isDirectory()) { throw new Exception(String.format("A non-directory already exists at [%s]", name)); }
		return f;
	}

	@Override
	protected AlfImportContext constructContext(String rootId, CmfType rootType, AlfRoot session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CmfTypeMapper typeMapper) {
		return new AlfImportContext(this, getSettings(), rootId, rootType, session, output, typeMapper, getEngine()
			.getTranslator(), objectStore, contentStore);
	}

	@Override
	protected String calculateProductName(AlfRoot session) throws Exception {
		return "XmlMetadata";
	}

	@Override
	protected String calculateProductVersion(AlfRoot session) throws Exception {
		return "1.0";
	}
}