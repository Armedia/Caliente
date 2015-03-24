/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.documentum.DctmSessionWrapper;
import com.armedia.cmf.engine.documentum.common.DctmSpecialValues;
import com.armedia.cmf.engine.exporter.ExportContextFactory;
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
public class DctmExportContextFactory
	extends
ExportContextFactory<IDfSession, DctmSessionWrapper, IDfPersistentObject, IDfValue, DctmExportContext, DctmExportEngine> {

	private final DctmSpecialValues specialValues;

	DctmExportContextFactory(DctmExportEngine engine, CfgTools settings) {
		super(engine, settings);
		this.specialValues = new DctmSpecialValues(settings);
	}

	@Override
	protected DctmExportContext constructContext(String rootId, StoredObjectType rootType, IDfSession session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore streamStore) {
		return new DctmExportContext(this, rootId, rootType, session, output);
	}

	public final DctmSpecialValues getSpecialValues() {
		return this.specialValues;
	}
}