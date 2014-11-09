/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class DctmExportFolder extends DctmExportSysObject<IDfFolder> {

	protected DctmExportFolder(DctmExportEngine engine) {
		super(engine, DctmObjectType.FOLDER);
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfFolder folder) throws DfException {
		return folder.getFolderPath(0);
	}

}