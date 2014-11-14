/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

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
public class DctmExportContextFactory extends
ContextFactory<IDfSession, IDfPersistentObject, IDfValue, DctmExportContext> {
	private final DctmExportEngine engine;

	DctmExportContextFactory(DctmExportEngine engine) {
		this.engine = engine;
	}

	@Override
	protected DctmExportContext constructContext(String rootId, StoredObjectType rootType, IDfSession session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore streamStore) {
		return new DctmExportContext(this.engine, getSettings(), rootId, rootType, session, output);
	}
}