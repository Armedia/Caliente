/**
 *
 */

package com.armedia.caliente.engine.sharepoint.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportContext;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.store.CmfArchetype;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author diego
 *
 */
public class ShptExportContext extends ExportContext<ShptSession, CmfValue, ShptExportContextFactory> {

	public ShptExportContext(ShptExportContextFactory factory, CfgTools settings, String rootId, CmfArchetype rootType,
		ShptSession session, Logger output, WarningTracker warningTracker) {
		super(factory, settings, rootId, rootType, session, output, warningTracker);
	}

}