/**
 *
 */

package com.armedia.cmf.documentum.engine.importer;

import org.slf4j.Logger;

import com.armedia.cmf.documentum.engine.DctmTranslator;
import com.armedia.cmf.engine.importer.ImportContext;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
class DctmImportContext extends ImportContext<IDfSession, IDfPersistentObject, IDfValue> {

	DctmImportContext(DctmImportEngine engine, String rootId, StoredObjectType rootType, IDfSession session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore streamStore) {
		super(engine, rootId, rootType, session, output, DctmTranslator.INSTANCE, objectStore, streamStore);
	}

}