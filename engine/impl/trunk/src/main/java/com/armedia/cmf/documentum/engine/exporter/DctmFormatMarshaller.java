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
public class DctmFormatMarshaller extends DctmExportDelegate<IDfFormat> {

	protected DctmFormatMarshaller() {
		super(DctmObjectType.FORMAT);
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfFormat format) throws DfException {
		return format.getName();
	}

}