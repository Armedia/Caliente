package com.armedia.cmf.engine.local.importer;

import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportDelegateFactory extends
ImportDelegateFactory<LocalRoot, LocalSessionWrapper, StoredValue, LocalImportContext, LocalImportEngine> {

	public LocalImportDelegateFactory(LocalImportEngine engine, CfgTools configuration) {
		super(engine, configuration);
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