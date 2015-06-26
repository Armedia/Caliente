/**
 *
 */

package com.armedia.cmf.engine.sharepoint.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContextFactory;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionWrapper;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author diego
 *
 */
public class ShptExportContextFactory extends
	ExportContextFactory<ShptSession, ShptSessionWrapper, CmfValue, ShptExportContext, ShptExportEngine> {

	ShptExportContextFactory(ShptExportEngine engine, CfgTools settings, ShptSession session) throws Exception {
		super(engine, settings, session);
	}

	@Override
	protected ShptExportContext constructContext(String rootId, CmfType rootType, ShptSession session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?> contentStore, CmfTypeMapper typeMapper) {
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