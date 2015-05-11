package com.armedia.cmf.engine.local.importer;

import com.armedia.cmf.engine.importer.ImportDelegate;
import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportDelegateFactory extends
	ImportDelegateFactory<LocalRoot, LocalSessionWrapper, StoredValue, LocalImportContext, LocalImportEngine> {

	public LocalImportDelegateFactory(LocalImportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	@Override
	protected ImportDelegate<?, LocalRoot, LocalSessionWrapper, StoredValue, LocalImportContext, ?, LocalImportEngine> newImportDelegate(
		StoredObject<?> storedObject) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}