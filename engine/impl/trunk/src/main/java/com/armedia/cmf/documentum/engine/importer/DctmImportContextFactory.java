/**
 *
 */

package com.armedia.cmf.documentum.engine.importer;

import org.slf4j.Logger;

import com.armedia.cmf.documentum.engine.common.DctmSpecialValues;
import com.armedia.cmf.engine.ContextFactory;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmImportContextFactory extends
	ContextFactory<IDfSession, IDfPersistentObject, IDfValue, DctmImportContext, DctmImportEngine> {
	private final DctmSpecialValues specialValues;

	DctmImportContextFactory(DctmImportEngine engine, CfgTools cfg) {
		super(engine, cfg);
		this.specialValues = new DctmSpecialValues(cfg);
	}

	@Override
	protected DctmImportContext constructContext(String rootId, StoredObjectType rootType, IDfSession session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore contentStore) {
		return new DctmImportContext(this, rootId, rootType, session, output, objectStore, contentStore);
	}

	public final DctmSpecialValues getSpecialValues() {
		return this.specialValues;
	}
}