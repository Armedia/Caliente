package com.armedia.cmf.engine.local.importer;

import java.io.File;

import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportDelegateFactory extends
	ImportDelegateFactory<File, LocalSessionWrapper, StoredValue, LocalImportContext, LocalImportEngine> {

	public LocalImportDelegateFactory(LocalImportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}
}