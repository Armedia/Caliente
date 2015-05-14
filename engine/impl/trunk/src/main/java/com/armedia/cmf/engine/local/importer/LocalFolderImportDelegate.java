package com.armedia.cmf.engine.local.importer;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.cmf.storage.StoredValueDecoderException;

public class LocalFolderImportDelegate extends LocalImportDelegate {

	protected LocalFolderImportDelegate(LocalImportDelegateFactory factory, StoredObject<StoredValue> storedObject)
		throws Exception {
		super(factory, storedObject);
	}

	@Override
	protected ImportOutcome importObject(ObjectStorageTranslator<StoredValue> translator, LocalImportContext ctx)
		throws ImportException, StorageException, StoredValueDecoderException {
		return null;
	}
}