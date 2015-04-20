package com.armedia.cmf.engine.cmis.importer;

import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.importer.ImportContextFactory;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;

public class CmisImportContextFactory extends
	ImportContextFactory<Session, CmisSessionWrapper, Property<?>, CmisImportContext, CmisImportEngine, Folder> {

	CmisImportContextFactory(CmisImportEngine engine, CfgTools settings) {
		super(engine, settings);
	}

	@Override
	protected CmisImportContext constructContext(String rootId, StoredObjectType rootType, Session session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore streamStore) {
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