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

	protected DctmExportStore(DctmExportDelegateFactory factory, IDfStore store) throws Exception {
		super(factory, IDfStore.class, store);
	}

	DctmExportStore(DctmExportDelegateFactory factory, IDfPersistentObject store) throws Exception {
		this(factory, DctmExportDelegate.staticCast(IDfStore.class, store));
	}

	@Override
	protected String calculateLabel(IDfStore store) throws Exception {
		return store.getName();
	}

	@Override
	protected String calculateName(IDfStore store) throws Exception {
		return store.getName();
	}
}