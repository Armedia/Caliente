/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class DctmTypeMarshaller extends DctmExportDelegate<IDfType> {

	protected DctmTypeMarshaller() {
		super(DctmObjectType.TYPE);
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfType type) throws DfException {
		String superName = type.getSuperName();
		if ((superName != null) && (superName.length() > 0)) {
			superName = String.format(" (extends %s)", superName);
		} else {
			superName = "";
		}
		return String.format("%s%s", type.getName(), superName);
	}

}