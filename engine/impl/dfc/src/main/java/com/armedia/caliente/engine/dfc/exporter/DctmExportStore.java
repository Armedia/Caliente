/**
 *
 */

package com.armedia.caliente.engine.dfc.exporter;

import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.content.IDfStore;

/**
 * @author diego
 *
 */
public class DctmExportStore extends DctmExportDelegate<IDfStore> {

	protected DctmExportStore(DctmExportDelegateFactory factory, IDfSession session, IDfStore store) throws Exception {
		super(factory, session, IDfStore.class, store);
	}

	DctmExportStore(DctmExportDelegateFactory factory, IDfSession session, IDfPersistentObject store) throws Exception {
		this(factory, session, DctmExportDelegate.staticCast(IDfStore.class, store));
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfStore store) throws Exception {
		return store.getName();
	}

	@Override
	protected String calculateName(IDfSession session, IDfStore store) throws Exception {
		return store.getName();
	}
}