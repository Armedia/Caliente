/**
 *
 */

package com.armedia.caliente.engine.dfc.exporter;

import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;

/**
 * @author diego
 *
 */
public class DctmExportFormat extends DctmExportDelegate<IDfFormat> {

	protected DctmExportFormat(DctmExportDelegateFactory factory, IDfSession session, IDfFormat format)
		throws Exception {
		super(factory, session, IDfFormat.class, format);
	}

	DctmExportFormat(DctmExportDelegateFactory factory, IDfSession session, IDfPersistentObject format)
		throws Exception {
		this(factory, session, DctmExportDelegate.staticCast(IDfFormat.class, format));
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfFormat format) throws Exception {
		return format.getName();
	}

	@Override
	protected String calculateName(IDfSession session, IDfFormat format) throws Exception {
		return format.getName();
	}
}