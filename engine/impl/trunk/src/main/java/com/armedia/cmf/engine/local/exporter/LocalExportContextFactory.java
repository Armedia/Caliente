package com.armedia.cmf.engine.local.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContextFactory;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportContextFactory extends
ExportContextFactory<LocalRoot, LocalSessionWrapper, StoredValue, LocalExportContext, LocalExportEngine> {

	protected LocalExportContextFactory(LocalExportEngine engine, CfgTools settings) {
		super(engine, settings);
	}

	@Override
	protected LocalExportContext constructContext(String rootId, StoredObjectType rootType, LocalRoot session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore<?> contentStore) {
		return new LocalExportContext(this, getSettings(), rootId, rootType, session, output);
	}
}