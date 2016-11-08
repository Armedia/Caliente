/**
 *
 */

package com.armedia.caliente.engine.sharepoint.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.exporter.ExportContextFactory;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.engine.sharepoint.ShptSessionWrapper;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author diego
 *
 */
public class ShptExportContextFactory
	extends ExportContextFactory<ShptSession, ShptSessionWrapper, CmfValue, ShptExportContext, ShptExportEngine> {

	ShptExportContextFactory(ShptExportEngine engine, CfgTools settings, ShptSession session) throws Exception {
		super(engine, settings, session);
	}

	@Override
	protected ShptExportContext constructContext(String rootId, CmfType rootType, ShptSession session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CmfTypeMapper typeMapper,
		int batchPosition) {
		return new ShptExportContext(this, getSettings(), rootId, rootType, session, output);
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