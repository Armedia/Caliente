/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.documentum.fc.client.IDfDocument;

/**
 * @author diego
 *
 */
public class DctmExportDocument extends DctmExportSysObject<IDfDocument> {

	protected DctmExportDocument() {
		super(DctmObjectType.DOCUMENT);
	}

}