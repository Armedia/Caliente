package com.armedia.cmf.engine.local.importer;

import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.engine.local.common.RootPath;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportDelegateFactory extends
ImportDelegateFactory<RootPath, LocalSessionWrapper, StoredValue, LocalImportContext, LocalImportEngine> {

	public LocalImportDelegateFactory(LocalImportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}
}