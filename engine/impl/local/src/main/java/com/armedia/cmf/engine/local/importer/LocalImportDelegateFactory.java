package com.armedia.cmf.engine.local.importer;

import java.io.IOException;

import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.engine.local.common.LocalSetting;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportDelegateFactory
	extends ImportDelegateFactory<LocalRoot, LocalSessionWrapper, CmfValue, LocalImportContext, LocalImportEngine> {

	private final boolean includeAllVersions;
	private final boolean failOnCollisions;

	public LocalImportDelegateFactory(LocalImportEngine engine, CfgTools configuration) throws IOException {
		super(engine, configuration);
		this.includeAllVersions = configuration.getBoolean(LocalSetting.INCLUDE_ALL_VERSIONS);
		this.failOnCollisions = configuration.getBoolean(LocalSetting.FAIL_ON_COLLISIONS);
	}

	public final boolean isIncludeAllVersions() {
		return this.includeAllVersions;
	}

	public final boolean isFailOnCollisions() {
		return this.failOnCollisions;
	}

	@Override
	protected LocalImportDelegate newImportDelegate(CmfObject<CmfValue> storedObject) throws Exception {
		switch (storedObject.getType()) {
			case DOCUMENT:
				return new LocalDocumentImportDelegate(this, storedObject);
			case FOLDER:
				return new LocalFolderImportDelegate(this, storedObject);
			default:
				return null;
		}
	}
}