/**
 *
 */

package com.armedia.cmf.documentum.engine.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.ContextFactory;
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
public class DctmImportContextFactory extends
	ContextFactory<IDfSession, IDfPersistentObject, IDfValue, DctmImportContext> {
	private final DctmImportEngine engine;

	DctmImportContextFactory(DctmImportEngine engine) {
		this.engine = engine;
	}

	@Override
	protected DctmImportContext constructContext(String rootId, StoredObjectType rootType, IDfSession session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore contentStore) {
		return new DctmImportContext(this.engine, getSettings(), rootId, rootType, session, output, objectStore,
			contentStore);
	}
}