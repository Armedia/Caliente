/**
 *
 */

package com.armedia.cmf.engine.sharepoint.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author diego
 *
 */
public class ShptExportContext extends ExportContext<ShptSession, CmfValue, ShptExportContextFactory> {

	public ShptExportContext(ShptExportContextFactory factory, CfgTools settings, String rootId, CmfType rootType,
		ShptSession session, Logger output) {
		super(factory, settings, rootId, rootType, session, output);
	}

}