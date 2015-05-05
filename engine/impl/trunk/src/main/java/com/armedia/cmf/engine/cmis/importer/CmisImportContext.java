package com.armedia.cmf.engine.cmis.importer;

import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;

import com.armedia.cmf.engine.importer.ImportContext;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;

public class CmisImportContext extends ImportContext<Session, StoredValue> {

	CmisImportContext(CmisImportContextFactory factory, String rootId, StoredObjectType rootType, Session session,
		Logger output, ObjectStorageTranslator<StoredValue> translator, ObjectStore<?, ?> objectStore,
		ContentStore streamStore) {
		super(factory, factory.getSettings(), rootId, rootType, session, output, translator, objectStore, streamStore);
	}
}