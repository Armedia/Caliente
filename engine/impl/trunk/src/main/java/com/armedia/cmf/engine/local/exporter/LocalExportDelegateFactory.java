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
		File root = LocalCommon.getRootDirectory(configuration);
		if (root == null) { throw new IllegalArgumentException("Must provide a root directory to work from"); }
		this.root = root;
	}

	public final File getRoot() {
		return this.root;
	}

	@Override
	protected LocalExportDelegate newExportDelegate(File session, StoredObjectType type, String searchKey)
		throws Exception {
		File f = new File(searchKey);
		File F = new File(session, searchKey).getCanonicalFile();
		if (!F.exists()) { return null; }
		return new LocalExportDelegate(this, f);
	}
}