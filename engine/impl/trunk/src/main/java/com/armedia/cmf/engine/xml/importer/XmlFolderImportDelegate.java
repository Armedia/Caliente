package com.armedia.cmf.engine.xml.importer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class XmlFolderImportDelegate extends XmlImportDelegate {

	protected XmlFolderImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject);
	}

	@Override
	protected Collection<ImportOutcome> doImportObject(CmfAttributeTranslator<CmfValue> translator,
		XmlImportContext ctx) throws ImportException, CmfStorageException, CmfValueDecoderException {
		File targetFile;
		try {
			targetFile = getTargetFile(ctx);
		} catch (IOException e) {
			throw new ImportException(String.format("Failed to calculate the target file for folder [%s](%s)",
				this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}

		if (targetFile.isDirectory()) { return Collections.singleton(new ImportOutcome(ImportResult.DUPLICATE,
			getNewId(targetFile), targetFile.getAbsolutePath())); }

		final boolean created = targetFile.mkdirs();
		if (!targetFile.isDirectory()) { throw new ImportException(String.format(
			"Failed to create the directory (and parents) at [%s] for folder [%s](%s)", targetFile,
			this.cmfObject.getLabel(), this.cmfObject.getId())); }
		if (!targetFile.exists()) { throw new ImportException(String.format(
			"A non-folder object already exists at [%s] for folder [%s](%s)", targetFile, this.cmfObject.getLabel(),
			this.cmfObject.getId())); }

		try {
			applyAttributes(targetFile, translator);
		} catch (Exception e) {
			throw new ImportException(String.format(
				"Failed to apply attributes to the target file [%s] for folder [%s](%s)", targetFile,
				this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}
		return Collections.singleton(new ImportOutcome(created ? ImportResult.CREATED : ImportResult.UPDATED,
			getNewId(targetFile), targetFile.getAbsolutePath()));
	}
}