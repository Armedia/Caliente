package com.armedia.cmf.engine.local.importer;

import java.io.IOException;

import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.engine.local.common.Setting;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportDelegateFactory extends
ImportDelegateFactory<LocalRoot, LocalSessionWrapper, StoredValue, LocalImportContext, LocalImportEngine> {

	private final boolean includeAllVersions;

	public LocalImportDelegateFactory(LocalImportEngine engine, CfgTools configuration) throws IOException {
		super(engine, configuration);
		this.includeAllVersions = configuration.getBoolean(Setting.INCLUDE_ALL_VERSIONS);
	}

	public final boolean isIncludeAllVersions() {
		return this.includeAllVersions;
	}

	@Override
	protected LocalImportDelegate newImportDelegate(StoredObject<StoredValue> storedObject) throws Exception {
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