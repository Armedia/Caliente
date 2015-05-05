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
public class DctmExportFormat extends DctmExportDelegate<IDfFormat> {

	protected DctmExportFormat(DctmExportDelegateFactory factory, IDfFormat format) throws Exception {
		super(factory, IDfFormat.class, format);
	}

	DctmExportFormat(DctmExportDelegateFactory factory, IDfPersistentObject format) throws Exception {
		this(factory, DctmExportDelegate.staticCast(IDfFormat.class, format));
	}

	@Override
	protected String calculateLabel(IDfFormat format) throws Exception {
		return format.getName();
	}
}