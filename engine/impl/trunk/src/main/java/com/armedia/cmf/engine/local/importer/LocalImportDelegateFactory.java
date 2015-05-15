package com.armedia.cmf.engine.local.importer;

import java.io.File;
import java.io.IOException;

import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.engine.importer.ImportSetting;
import com.armedia.cmf.engine.local.common.LocalCommon;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportDelegateFactory extends
	ImportDelegateFactory<LocalRoot, LocalSessionWrapper, StoredValue, LocalImportContext, LocalImportEngine> {

	private LocalRoot root;

	public LocalImportDelegateFactory(LocalImportEngine engine, CfgTools configuration) throws IOException {
		super(engine, configuration);
		File root = LocalCommon.getRootDirectory(configuration);
		if (root == null) { throw new IllegalArgumentException("Must provide a root directory to work from"); }
		String tgtPath = configuration.getString(ImportSetting.TARGET_LOCATION);
		this.root = new LocalRoot(new File(root, tgtPath).getCanonicalFile());
	}

	public LocalRoot getRoot() {
		return this.root;
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