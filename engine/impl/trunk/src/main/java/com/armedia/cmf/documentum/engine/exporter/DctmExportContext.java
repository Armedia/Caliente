/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContext;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
class DctmExportContext extends ExportContext<IDfSession, IDfPersistentObject, IDfValue> {

	DctmExportContext(String rootId, IDfSession session, Logger output) {
		super(rootId, session, output);
	}

}