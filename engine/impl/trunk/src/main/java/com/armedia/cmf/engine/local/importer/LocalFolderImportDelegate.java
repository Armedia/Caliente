package com.armedia.cmf.engine.local.importer;

import java.io.File;

import com.armedia.cmf.engine.importer.ImportDelegate;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.cmf.storage.StoredValueDecoderException;

public class LocalFolderImportDelegate
extends
ImportDelegate<File, LocalRoot, LocalSessionWrapper, StoredValue, LocalImportContext, LocalImportDelegateFactory, LocalImportEngine> {

	protected LocalFolderImportDelegate(LocalImportDelegateFactory factory, StoredObject<StoredValue> storedObject)
		throws Exception {
		super(factory, File.class, storedObject);
	}

	@Override
	protected ImportOutcome importObject(ObjectStorageTranslator<StoredValue> translator, LocalImportContext ctx)
		throws ImportException, StorageException, StoredValueDecoderException {
		return null;
	}
}