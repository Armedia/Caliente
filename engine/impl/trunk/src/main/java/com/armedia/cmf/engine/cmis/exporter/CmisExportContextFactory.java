package com.armedia.cmf.engine.cmis.exporter;

import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.exporter.ExportContextFactory;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class CmisExportContextFactory extends
	ExportContextFactory<Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportEngine> {

	CmisExportContextFactory(CmisExportEngine engine, CfgTools settings) {
		super(engine, settings);
	}

	@Override
	protected CmisExportContext constructContext(String rootId, CmfType rootType, Session session,
		Logger output, CmfObjectStore<?, ?> objectStore, CmfContentStore<?> streamStore) {
		return new CmisExportContext(this, rootId, rootType, session, output);
	}
}