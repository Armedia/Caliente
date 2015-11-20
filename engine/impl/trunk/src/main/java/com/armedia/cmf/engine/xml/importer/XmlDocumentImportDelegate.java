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

public class XmlDocumentImportDelegate extends XmlFSObjectImportDelegate {

	protected XmlDocumentImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject);
	}

	@Override
	protected Collection<ImportOutcome> doImportObject(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
		File targetFile;
		try {
			targetFile = getTargetFile(ctx);
		} catch (IOException e) {
			throw new ImportException(String.format("Failed to calculate the target file for document [%s](%s)",
				this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}

		File parent = targetFile.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}

		final boolean created;
		try {
			created = targetFile.createNewFile();
		} catch (IOException e) {
			throw new ImportException(String.format("Failed to create the new file [%s] for DOCUMENT [%s](%s)",
				targetFile.getAbsolutePath(), this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}

		if (!created) {
			if (!targetFile.isFile()) { throw new ImportException(String.format(
				"Failed to create the file (and parents) at [%s] for document [%s](%s)", targetFile,
				this.cmfObject.getLabel(), this.cmfObject.getId())); }
			if (!targetFile.exists()) { throw new ImportException(String.format(
				"A non-file object already exists at [%s] for document [%s](%s)", targetFile,
				this.cmfObject.getLabel(), this.cmfObject.getId())); }

			if (this.factory.isFailOnCollisions()) { throw new ImportException(String.format(
				"A file already exists at [%s] for document [%s](%s)", targetFile, this.cmfObject.getLabel(),
				this.cmfObject.getId())); }
		}

		// Copy the contents over...
		/*
		List<CmfContentInfo> contents;
		try {
			contents = ctx.getContentInfo(this.cmfObject);
		} catch (Exception e) {
			throw new ImportException(String.format(
				"Failed to obtain the list of content streams for document [%s](%s)", this.cmfObject.getLabel(),
				this.cmfObject.getId()), e);
		}

		if (!contents.isEmpty()) {
			CmfContentInfo info = contents.get(0);
			CmfContentStore<?>.Handle h = ctx.getContentStore().getHandle(translator, this.cmfObject,
				info.getQualifier());
			File src = h.getFile();
			if (src != null) {
				try {
					Files.copy(src.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new ImportException(String.format(
						"Failed to copy the contents from [%s] to [%s] for document [%s](%s)", src, targetFile,
						this.cmfObject.getLabel(), this.cmfObject.getId()), e);
				}
			} else {
				InputStream in = null;
				try {
					in = h.openInput();
					Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new ImportException(String.format(
						"Failed to copy the default content object into [%s] for document [%s](%s)", targetFile,
						this.cmfObject.getLabel(), this.cmfObject.getId()), e);
				} finally {
					IOUtils.closeQuietly(in);
				}
			}
		}
		 */
		return Collections.singleton(new ImportOutcome(created ? ImportResult.CREATED : ImportResult.UPDATED,
			getNewId(targetFile), targetFile.getAbsolutePath()));
	}
}