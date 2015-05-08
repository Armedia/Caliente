package com.armedia.cmf.engine.local.exporter;

import java.io.File;

import com.armedia.cmf.engine.exporter.ExportDelegateFactory;
import com.armedia.cmf.engine.local.common.LocalCommon;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.engine.local.common.LocalFile;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportDelegateFactory extends
ExportDelegateFactory<LocalRoot, LocalSessionWrapper, StoredValue, LocalExportContext, LocalExportEngine> {

	private final LocalRoot root;

	protected LocalExportDelegateFactory(LocalExportEngine engine, CfgTools configuration) throws Exception {
		super(engine, configuration);
		File root = LocalCommon.getRootDirectory(configuration);
		if (root == null) { throw new IllegalArgumentException("Must provide a root directory to work from"); }
		this.root = new LocalRoot(root);
	}

	public final LocalRoot getRoot() {
		return this.root;
	}

	@Override
	protected LocalExportDelegate newExportDelegate(LocalRoot session, StoredObjectType type, String searchKey)
		throws Exception {
		return new LocalExportDelegate(this, LocalFile.newFromSafePath(session, searchKey));
	}
}