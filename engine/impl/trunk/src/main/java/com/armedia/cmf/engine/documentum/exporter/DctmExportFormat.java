/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPersistentObject;

/**
 * @author diego
 *
 */
public class DctmExportFormat extends DctmExportAbstract<IDfFormat> {

	protected DctmExportFormat(DctmExportEngine engine, IDfFormat format) throws Exception {
		super(engine, IDfFormat.class, format);
	}

	DctmExportFormat(DctmExportEngine engine, IDfPersistentObject format) throws Exception {
		this(engine, DctmExportAbstract.staticCast(IDfFormat.class, format));
	}

	@Override
	protected String calculateLabel(IDfFormat format) throws Exception {
		return format.getName();
	}
}