package com.armedia.cmf.engine.local.exporter;

import java.io.File;

import com.armedia.cmf.engine.exporter.ExportDelegateFactory;
import com.armedia.cmf.engine.local.common.LocalCommon;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.engine.local.common.RootPath;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportDelegateFactory extends
	ExportDelegateFactory<RootPath, LocalSessionWrapper, StoredValue, LocalExportContext, LocalExportEngine> {

	private final RootPath root;

	protected LocalExportDelegateFactory(LocalExportEngine engine, CfgTools configuration) throws Exception {
		super(engine, configuration);
		File root = LocalCommon.getRootDirectory(configuration);
		if (root == null) { throw new IllegalArgumentException("Must provide a root directory to work from"); }
		this.root = new RootPath(root);
	}

	public final RootPath getRoot() {
		return this.root;
	}

	@Override
	protected LocalExportDelegate newExportDelegate(RootPath session, StoredObjectType type, String searchKey)
		throws Exception {
		return null;
	}
}