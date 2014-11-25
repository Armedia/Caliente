/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.content.IDfStore;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class DctmExportStore extends DctmExportAbstract<IDfStore> {

	protected DctmExportStore(DctmExportEngine engine) {
		super(engine, DctmObjectType.STORE);
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfStore store) throws DfException {
		return store.getName();
	}
}