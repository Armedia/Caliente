package com.armedia.cmf.engine.local.importer;

import java.io.File;
import java.io.IOException;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.AttributeTranslator;
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
	protected ImportOutcome doImportObject(AttributeTranslator<StoredValue> translator, LocalImportContext ctx)
		throws ImportException, StorageException, StoredValueDecoderException {
		File targetFile;
		try {
			targetFile = getTargetFile(ctx);
		} catch (IOException e) {
			throw new ImportException(String.format("Failed to calculate the target file for folder [%s](%s)",
				this.storedObject.getLabel(), this.storedObject.getId()), e);
		}

		if (targetFile.isDirectory()) { return new ImportOutcome(ImportResult.DUPLICATE, getNewId(targetFile),
			targetFile.getAbsolutePath()); }

		final boolean created = targetFile.mkdirs();
		if (!targetFile.isDirectory()) { throw new ImportException(String.format(
			"Failed to create the directory (and parents) at [%s] for folder [%s](%s)", targetFile,
			this.storedObject.getLabel(), this.storedObject.getId())); }
		if (!targetFile.exists()) { throw new ImportException(String.format(
			"A non-folder object already exists at [%s] for folder [%s](%s)", targetFile, this.storedObject.getLabel(),
			this.storedObject.getId())); }

		try {
			applyAttributes(targetFile, translator);
		} catch (Exception e) {
			throw new ImportException(String.format(
				"Failed to apply attributes to the target file [%s] for folder [%s](%s)", targetFile,
				this.storedObject.getLabel(), this.storedObject.getId()), e);
		}
		return new ImportOutcome(created ? ImportResult.CREATED : ImportResult.UPDATED, getNewId(targetFile),
			targetFile.getAbsolutePath());
	}
}