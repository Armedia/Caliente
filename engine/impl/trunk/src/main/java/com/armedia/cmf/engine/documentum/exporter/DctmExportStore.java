/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.content.IDfStore;

/**
 * @author diego
 *
 */
public class DctmExportStore extends DctmExportDelegate<IDfStore> {

	protected DctmExportStore(DctmExportEngine engine, IDfStore store, CfgTools configuration) throws Exception {
		super(engine, IDfStore.class, store, configuration);
	}

	DctmExportStore(DctmExportEngine engine, IDfPersistentObject store, CfgTools configuration) throws Exception {
		this(engine, DctmExportDelegate.staticCast(IDfStore.class, store), configuration);
	}

	@Override
	protected String calculateLabel(IDfStore store) throws Exception {
		return store.getName();
	}
}