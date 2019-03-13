/**
 *
 */

package com.armedia.caliente.engine.sharepoint.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportContextFactory;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.engine.sharepoint.ShptSessionWrapper;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author diego
 *
 */
public class ShptExportContextFactory
	extends ExportContextFactory<ShptSession, ShptSessionWrapper, CmfValue, ShptExportContext, ShptExportEngine> {

	ShptExportContextFactory(ShptExportEngine engine, CfgTools settings, ShptSession session,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, Logger output,
		WarningTracker warningTracker) throws Exception {
		super(engine, settings, session, objectStore, contentStore, output, warningTracker);
	}

	@Override
	protected ShptExportContext constructContext(String rootId, CmfObject.Archetype rootType, ShptSession session,
		int batchPosition) {
		return new ShptExportContext(this, getSettings(), rootId, rootType, session, getOutput(), getWarningTracker());
	}

	@Override
	protected String calculateProductName(ShptSession session) throws Exception {
		return "Sharepoint";
	}

	@Override
	protected String calculateProductVersion(ShptSession session) throws Exception {
		return session.getContextInfo().getLibraryVersion();
	}
}