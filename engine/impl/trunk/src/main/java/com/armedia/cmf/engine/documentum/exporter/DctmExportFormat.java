/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPersistentObject;

/**
 * @author diego
 *
 */
public class DctmExportFormat extends DctmExportDelegate<IDfFormat> {

	protected DctmExportFormat(DctmExportEngine engine, IDfFormat format, CfgTools configuration) throws Exception {
		super(engine, IDfFormat.class, format, configuration);
	}

	DctmExportFormat(DctmExportEngine engine, IDfPersistentObject format, CfgTools configuration) throws Exception {
		this(engine, DctmExportDelegate.staticCast(IDfFormat.class, format), configuration);
	}

	@Override
	protected String calculateLabel(IDfFormat format, CfgTools configuration) throws Exception {
		return format.getName();
	}
}