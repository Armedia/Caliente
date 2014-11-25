/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class DctmExportFormat extends DctmExportAbstract<IDfFormat> {

	protected DctmExportFormat(DctmExportEngine engine) {
		super(engine, DctmObjectType.FORMAT);
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfFormat format) throws DfException {
		return format.getName();
	}
}