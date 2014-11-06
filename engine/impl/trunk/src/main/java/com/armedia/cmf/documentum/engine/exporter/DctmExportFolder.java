/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.documentum.fc.client.IDfFolder;

/**
 * @author diego
 *
 */
public class DctmExportFolder extends DctmExportSysObject<IDfFolder> {

	protected DctmExportFolder() {
		super(DctmObjectType.FOLDER);
	}

}