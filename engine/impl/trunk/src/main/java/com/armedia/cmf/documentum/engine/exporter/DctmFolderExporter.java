/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class DctmFolderExporter extends DctmExporter<IDfUser> {

	protected DctmFolderExporter() {
		super(DctmObjectType.FOLDER);
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfUser user) throws DfException {
		return user.getUserName();
	}

}