package com.armedia.caliente.engine.xml.importer;

import java.io.File;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportContextFactory;
import com.armedia.caliente.engine.xml.common.XmlRoot;
import com.armedia.caliente.engine.xml.common.XmlSessionWrapper;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class XmlImportContextFactory
	extends ImportContextFactory<XmlRoot, XmlSessionWrapper, CmfValue, XmlImportContext, XmlImportEngine, File> {

	protected XmlImportContextFactory(XmlImportEngine engine, CfgTools settings, XmlRoot root,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
		super(engine, settings, root, objectStore, contentStore, transformer, output, warningTracker);
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
	protected XmlImportContext constructContext(String rootId, CmfObject.Archetype rootType, XmlRoot session, int historyPosition) {
		return new XmlImportContext(this, getSettings(), rootId, rootType, session, getOutput(), getWarningTracker(),
			getTransformer(), getEngine().getTranslator(), getObjectStore(), getContentStore(), historyPosition);
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