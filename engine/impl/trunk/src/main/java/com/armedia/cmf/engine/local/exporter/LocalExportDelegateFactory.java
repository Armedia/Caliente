package com.armedia.cmf.engine.local.exporter;

import java.io.File;

import com.armedia.cmf.engine.exporter.ExportDelegateFactory;
import com.armedia.cmf.engine.local.common.LocalCommon;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportDelegateFactory extends
	ExportDelegateFactory<File, LocalSessionWrapper, StoredValue, LocalExportContext, LocalExportEngine> {

	private final File root;

	protected LocalExportDelegateFactory(LocalExportEngine engine, CfgTools configuration) throws Exception {
		super(engine, configuration);
		this.root = LocalCommon.getRootDirectory(configuration);
		if (this.root == null) { throw new IllegalArgumentException("Must provide a root directory to work from"); }
	}

	public final File getRoot() {
		return this.root;
	}

	@Override
	protected LocalExportDelegate newExportDelegate(File session, StoredObjectType type, String searchKey)
		throws Exception {
		return null;
	}
}