package com.armedia.caliente.engine.local.exporter;

import java.io.File;

import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.common.LocalFile;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.engine.local.common.LocalSetting;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportDelegateFactory
	extends ExportDelegateFactory<LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportEngine> {

	private final LocalRoot root;
	private final boolean copyContent;
	private final boolean includeAllVersions;

	protected LocalExportDelegateFactory(LocalExportEngine engine, CfgTools configuration) throws Exception {
		super(engine, configuration);
		File root = LocalCommon.getRootDirectory(configuration);
		if (root == null) { throw new IllegalArgumentException("Must provide a root directory to work from"); }
		this.root = new LocalRoot(root);
		this.copyContent = configuration.getBoolean(LocalSetting.COPY_CONTENT);
		this.includeAllVersions = configuration.getBoolean(LocalSetting.INCLUDE_ALL_VERSIONS);
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
	protected LocalExportDelegate newExportDelegate(LocalRoot session, CmfType type, String searchKey)
		throws Exception {
		return new LocalExportDelegate(this, LocalFile.newFromSafePath(session, searchKey));
	}
}