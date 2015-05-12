package com.armedia.cmf.engine.cmis.exporter;

import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.exporter.ExportContextFactory;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class CmisExportContextFactory extends
	ExportContextFactory<Session, CmisSessionWrapper, StoredValue, CmisExportContext, CmisExportEngine> {

	CmisExportContextFactory(CmisExportEngine engine, CfgTools settings) {
		super(engine, settings);
	}

	@Override
	protected CmisExportContext constructContext(String rootId, StoredObjectType rootType, Session session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore<?> streamStore) {
		return new CmisExportContext(this, rootId, rootType, session, output);
	}
}