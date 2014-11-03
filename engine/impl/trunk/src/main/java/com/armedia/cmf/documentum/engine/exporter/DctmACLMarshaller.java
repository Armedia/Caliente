/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class DctmACLMarshaller extends DctmExportDelegate<IDfACL> {

	protected DctmACLMarshaller() {
		super(DctmObjectType.ACL);
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfACL acl) throws DfException {
		return String.format("%s::%s", acl.getDomain(), acl.getObjectName());
	}

}