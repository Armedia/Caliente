package com.armedia.cmf.engine.local.importer;

import java.io.IOException;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.cmf.storage.StoredValueDecoderException;

public class LocalDocumentImportDelegate extends LocalImportDelegate {

	protected LocalDocumentImportDelegate(LocalImportDelegateFactory factory, StoredObject<StoredValue> storedObject)
		throws Exception {
		super(factory, storedObject);
	}

	@Override
	protected ImportOutcome importObject(ObjectStorageTranslator<StoredValue> translator, LocalImportContext ctx)
		throws ImportException, StorageException, StoredValueDecoderException {

		this.targetFile.mkdirs();

		final boolean created;
		try {
			created = this.targetFile.createNewFile();
		} catch (IOException e) {
			throw new ImportException(String.format("Failed to create the new file [%s] for DOCUMENT [%s](%s)",
				this.targetFile.getAbsolutePath(), this.storedObject.getLabel(), this.storedObject.getId()), e);
		}

		if (!created) {
			if (!this.targetFile.isFile()) { throw new ImportException(String.format(
				"Failed to create the file (and parents) at [%s] for document [%s](%s)", this.targetFile,
				this.storedObject.getLabel(), this.storedObject.getId())); }
			if (!this.targetFile.exists()) { throw new ImportException(String.format(
				"A non-file object already exists at [%s] for document [%s](%s)", this.targetFile,
				this.storedObject.getLabel(), this.storedObject.getId())); }
			return new ImportOutcome(ImportResult.DUPLICATE, this.newId, this.targetFile.getAbsolutePath());
		}

		// Copy the contents over...
		try {
			applyAttributes(translator);
		} catch (Exception e) {
			throw new ImportException(String.format(
				"Failed to apply attributes to the target file [%s] for %s [%s](%s)", this.targetFile,
				this.storedObject.getType(), this.storedObject.getLabel(), this.storedObject.getId()), e);
		}
		return new ImportOutcome(created ? ImportResult.CREATED : ImportResult.UPDATED, this.newId,
			this.targetFile.getAbsolutePath());
	}
}