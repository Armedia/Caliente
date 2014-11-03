/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class DctmGroupMarshaller extends DctmExportDelegate<IDfGroup> {

	protected DctmGroupMarshaller() {
		super(DctmObjectType.GROUP);
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfGroup group) throws DfException {
		return group.getGroupName();
	}

}