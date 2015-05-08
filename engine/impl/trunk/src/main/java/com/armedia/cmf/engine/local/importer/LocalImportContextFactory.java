package com.armedia.cmf.engine.local.importer;

import java.io.File;

import org.slf4j.Logger;

import com.armedia.cmf.engine.importer.ImportContextFactory;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.engine.local.common.RootPath;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportContextFactory extends
	ImportContextFactory<RootPath, LocalSessionWrapper, StoredValue, LocalImportContext, LocalImportEngine, File> {

	protected LocalImportContextFactory(LocalImportEngine engine, CfgTools settings) {
		super(engine, settings);
	}

	@Override
	protected File locateFolder(RootPath session, String path) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected File createFolder(RootPath session, File parent, String name) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected LocalImportContext constructContext(String rootId, StoredObjectType rootType, RootPath session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore contentStore) {
		// TODO Auto-generated method stub
		return null;
	}
}