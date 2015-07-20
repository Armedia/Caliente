package com.armedia.cmf.engine.local.exporter;

import java.io.File;

import com.armedia.cmf.engine.exporter.ExportDelegateFactory;
import com.armedia.cmf.engine.local.common.LocalCommon;
import com.armedia.cmf.engine.local.common.LocalFile;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.engine.local.common.Setting;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportDelegateFactory extends
	ExportDelegateFactory<LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportEngine> {

	private final LocalRoot root;
	private final boolean copyContent;
	private final boolean includeAllVersions;

	protected LocalExportDelegateFactory(LocalExportEngine engine, CfgTools configuration) throws Exception {
		super(engine, configuration);
		File root = LocalCommon.getRootDirectory(configuration);
		if (root == null) { throw new IllegalArgumentException("Must provide a root directory to work from"); }
		this.root = new LocalRoot(root);
		this.copyContent = configuration.getBoolean(Setting.COPY_CONTENT);
		this.includeAllVersions = configuration.getBoolean(Setting.INCLUDE_ALL_VERSIONS);
	}

	public final LocalRoot getRoot() {
		return this.root;
	}

	public final boolean isCopyContent() {
		return this.copyContent;
	}

	public final boolean isIncludeAllVersions() {
		return this.includeAllVersions;
	}

	@Override
	protected LocalExportDelegate newExportDelegate(LocalRoot session, CmfType type, String searchKey) throws Exception {
		return new LocalExportDelegate(this, LocalFile.newFromSafePath(session, searchKey));
	}
}