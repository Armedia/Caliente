/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.content.IDfStore;

/**
 * @author diego
 *
 */
public class DctmExportStore extends DctmExportDelegate<IDfStore> {

	protected DctmExportStore(DctmExportEngine engine, IDfStore store) throws Exception {
		super(engine, IDfStore.class, store);
	}

	DctmExportStore(DctmExportEngine engine, IDfPersistentObject store) throws Exception {
		this(engine, DctmExportDelegate.staticCast(IDfStore.class, store));
	}

	@Override
	protected String calculateLabel(IDfStore store) throws Exception {
		return store.getName();
	}
}