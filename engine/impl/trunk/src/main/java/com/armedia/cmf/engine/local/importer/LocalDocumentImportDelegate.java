package com.armedia.cmf.engine.local.importer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.ContentStore;
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

		File parent = this.targetFile.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}

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

			try {
				if (isSameDatesAndOwners(translator)) { return new ImportOutcome(ImportResult.DUPLICATE, this.newId,
					this.targetFile.getAbsolutePath()); }
			} catch (Exception e) {
				throw new ImportException(String.format(
					"Failed to validate the dates and owners at [%s] for document [%s](%s)", this.targetFile,
					this.storedObject.getLabel(), this.storedObject.getId()), e);
			}
		}

		// Copy the contents over...
		List<ContentInfo> contents;
		try {
			contents = ctx.getContentInfo(this.storedObject);
		} catch (Exception e) {
			throw new ImportException(String.format(
				"Failed to obtain the list of content streams for document [%s](%s)", this.storedObject.getLabel(),
				this.storedObject.getId()), e);
		}

		if (!contents.isEmpty()) {
			ContentInfo info = contents.get(0);
			ContentStore<?>.Handle h = ctx.getContentStore().getHandle(translator, this.storedObject,
				info.getQualifier());
			File src = h.getFile();
			if (src != null) {
				try {
					Files.copy(src.toPath(), this.targetPath, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new ImportException(String.format(
						"Failed to copy the contents from [%s] to [%s] for document [%s](%s)", src, this.targetFile,
						this.storedObject.getLabel(), this.storedObject.getId()), e);
				}
			} else {
				InputStream in = null;
				try {
					in = h.openInput();
					Files.copy(in, this.targetPath, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new ImportException(String.format(
						"Failed to copy the default content object into [%s] for document [%s](%s)", this.targetFile,
						this.storedObject.getLabel(), this.storedObject.getId()), e);
				} finally {
					IOUtils.closeQuietly(in);
				}
			}
		}

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