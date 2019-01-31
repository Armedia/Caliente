package com.armedia.caliente.engine.local.importer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public class LocalFolderImportDelegate extends LocalImportDelegate {

	protected LocalFolderImportDelegate(LocalImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject);
	}

	@Override
	protected Collection<ImportOutcome> doImportObject(CmfAttributeTranslator<CmfValue> translator,
		LocalImportContext ctx) throws ImportException, CmfStorageException {
		File targetFile;
		try {
			targetFile = getTargetFile(ctx);
		} catch (IOException e) {
			throw new ImportException(String.format("Failed to calculate the target file for folder [%s](%s)",
				this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}

		if (targetFile.isDirectory()) {
			return Collections.singleton(
				new ImportOutcome(ImportResult.DUPLICATE, getNewId(targetFile), targetFile.getAbsolutePath()));
		}

		final boolean created = targetFile.mkdirs();
		if (!targetFile.isDirectory()) {
			throw new ImportException(
				String.format("Failed to create the directory (and parents) at [%s] for folder [%s](%s)", targetFile,
					this.cmfObject.getLabel(), this.cmfObject.getId()));
		}
		if (!targetFile.exists()) {
			throw new ImportException(String.format("A non-folder object already exists at [%s] for folder [%s](%s)",
				targetFile, this.cmfObject.getLabel(), this.cmfObject.getId()));
		}

		try {
			applyAttributes(targetFile, translator);
		} catch (Exception e) {
			throw new ImportException(
				String.format("Failed to apply attributes to the target file [%s] for folder [%s](%s)", targetFile,
					this.cmfObject.getLabel(), this.cmfObject.getId()),
				e);
		}
		return Collections.singleton(new ImportOutcome(created ? ImportResult.CREATED : ImportResult.UPDATED,
			getNewId(targetFile), targetFile.getAbsolutePath()));
	}
}