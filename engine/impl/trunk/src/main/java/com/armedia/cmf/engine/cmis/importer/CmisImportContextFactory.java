package com.armedia.cmf.engine.cmis.importer;

import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.importer.ImportContextFactory;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class CmisImportContextFactory extends
	ImportContextFactory<Session, CmisSessionWrapper, CmfValue, CmisImportContext, CmisImportEngine, Folder> {

	CmisImportContextFactory(CmisImportEngine engine, CfgTools settings) {
		super(engine, settings);
	}

	@Override
	protected CmisImportContext constructContext(String rootId, CmfType rootType, Session session,
		Logger output, CmfObjectStore<?, ?> objectStore, CmfContentStore<?> streamStore) {
		return new CmisImportContext(this, rootId, rootType, session, output, getEngine().getTranslator(), objectStore,
			streamStore);
	}

	@Override
	protected Folder locateFolder(Session session, String path) throws Exception {
		return null;
	}

	@Override
	protected Folder createFolder(Session session, Folder parent, String name) throws Exception {
		return null;
	}
}