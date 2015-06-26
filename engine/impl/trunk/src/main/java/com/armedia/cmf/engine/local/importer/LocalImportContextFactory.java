package com.armedia.cmf.engine.local.importer;

import java.io.File;

import org.slf4j.Logger;

import com.armedia.cmf.engine.importer.ImportContextFactory;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportContextFactory extends
	ImportContextFactory<LocalRoot, LocalSessionWrapper, CmfValue, LocalImportContext, LocalImportEngine, File> {

	protected LocalImportContextFactory(LocalImportEngine engine, CfgTools settings, LocalRoot root) throws Exception {
		super(engine, settings, root);
	}

	@Override
	protected File locateFolder(LocalRoot session, String path) throws Exception {
		File f = new File(session.getFile(), path).getCanonicalFile();
		if (f.exists() && f.isDirectory()) { return f; }
		return null;
	}

	@Override
	protected File createFolder(LocalRoot session, File parent, String name) throws Exception {
		File f = new File(parent, name);
		f.mkdirs();
		if (!f.exists()) { throw new Exception(String.format("Could not create the directory at [%s]", name)); }
		if (!f.isDirectory()) { throw new Exception(String.format("A non-directory already exists at [%s]", name)); }
		return f;
	}

	@Override
	protected LocalImportContext constructContext(String rootId, CmfType rootType, LocalRoot session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?> contentStore, CmfTypeMapper typeMapper) {
		return new LocalImportContext(this, getSettings(), rootId, rootType, session, output, typeMapper, getEngine()
			.getTranslator(), objectStore, contentStore);
	}

	@Override
	protected String calculateProductName(LocalRoot session) throws Exception {
		return "LocalFilesystem";
	}

	@Override
	protected String calculateProductVersion(LocalRoot session) throws Exception {
		return "1.0";
	}
}