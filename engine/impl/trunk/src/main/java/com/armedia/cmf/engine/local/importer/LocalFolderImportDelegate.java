package com.armedia.cmf.engine.local.importer;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
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
		String tgtPath = ctx.getTargetPath(this.targetFile.getPath());
		tgtPath.hashCode();

		if (this.targetFile.isDirectory()) { return new ImportOutcome(ImportResult.DUPLICATE, this.newId,
			this.targetFile.getAbsolutePath()); }

		final boolean created = this.targetFile.mkdirs();
		if (!this.targetFile.isDirectory()) { throw new ImportException(String.format(
			"Failed to create the directory (and parents) at [%s] for folder [%s](%s)", this.targetFile,
			this.storedObject.getLabel(), this.storedObject.getId())); }
		if (!this.targetFile.exists()) { throw new ImportException(String.format(
			"A non-folder object already exists at [%s] for folder [%s](%s)", this.targetFile,
			this.storedObject.getLabel(), this.storedObject.getId())); }

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