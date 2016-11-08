/**
 *
 */

package com.armedia.caliente.engine.sharepoint.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.exporter.ExportContext;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
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