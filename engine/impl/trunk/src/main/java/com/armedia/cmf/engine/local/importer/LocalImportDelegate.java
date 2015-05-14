package com.armedia.cmf.engine.local.importer;

import java.io.File;

import com.armedia.cmf.engine.importer.ImportDelegate;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;

public abstract class LocalImportDelegate
extends
ImportDelegate<File, LocalRoot, LocalSessionWrapper, StoredValue, LocalImportContext, LocalImportDelegateFactory, LocalImportEngine> {

	protected LocalImportDelegate(LocalImportDelegateFactory factory, StoredObject<StoredValue> storedObject)
		throws Exception {
		super(factory, File.class, storedObject);
	}

}