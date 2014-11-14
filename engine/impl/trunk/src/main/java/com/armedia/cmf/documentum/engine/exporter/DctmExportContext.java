/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
class DctmExportContext extends ExportContext<IDfSession, IDfPersistentObject, IDfValue> {

	DctmExportContext(DctmExportEngine engine, CfgTools settings, String rootId, StoredObjectType rootType,
		IDfSession session, Logger output) {
		super(engine, settings, rootId, rootType, session, output);
	}

}