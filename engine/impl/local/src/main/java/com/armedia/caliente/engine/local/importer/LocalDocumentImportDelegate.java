/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.local.importer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public class LocalDocumentImportDelegate extends LocalImportDelegate {

	protected LocalDocumentImportDelegate(LocalImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
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
			if (!targetFile.isFile()) {
				throw new ImportException(
					String.format("Failed to create the file (and parents) at [%s] for document [%s](%s)", targetFile,
						this.cmfObject.getLabel(), this.cmfObject.getId()));
			}
			if (!targetFile.exists()) {
				throw new ImportException(
					String.format("A non-file object already exists at [%s] for document [%s](%s)", targetFile,
						this.cmfObject.getLabel(), this.cmfObject.getId()));
			}

			if (this.factory.isFailOnCollisions()) {
				throw new ImportException(String.format("A file already exists at [%s] for document [%s](%s)",
					targetFile, this.cmfObject.getLabel(), this.cmfObject.getId()));
			}

			try {
				if (isSameDatesAndOwners(targetFile, translator)) {
					return Collections.singleton(
						new ImportOutcome(ImportResult.DUPLICATE, getNewId(targetFile), targetFile.getAbsolutePath()));
				}
			} catch (Exception e) {
				throw new ImportException(
					String.format("Failed to validate the dates and owners at [%s] for document [%s](%s)", targetFile,
						this.cmfObject.getLabel(), this.cmfObject.getId()),
					e);
			}
		}

		// Copy the contents over...
		List<CmfContentStream> contents;
		try {
			contents = ctx.getContentStreams(this.cmfObject);
		} catch (Exception e) {
			throw new ImportException(
				String.format("Failed to obtain the list of content streams for document [%s](%s)",
					this.cmfObject.getLabel(), this.cmfObject.getId()),
				e);
		}

		if (!contents.isEmpty()) {
			CmfContentStream info = contents.get(0);
			CmfContentStore<?, ?>.Handle h = ctx.getContentStore().getHandle(translator, this.cmfObject, info);
			final File src;
			try {
				src = h.getFile();
			} catch (IOException e) {
				throw new ImportException(String.format("Failed to obtain the content file for %s, content [%s]",
					this.cmfObject.getDescription(), info), e);
			}
			if (src != null) {
				try {
					Files.copy(src.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new ImportException(
						String.format("Failed to copy the contents from [%s] to [%s] for document [%s](%s)", src,
							targetFile, this.cmfObject.getLabel(), this.cmfObject.getId()),
						e);
				}
			} else {
				try (InputStream in = h.openInput()) {
					Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new ImportException(
						String.format("Failed to copy the default content object into [%s] for document [%s](%s)",
							targetFile, this.cmfObject.getLabel(), this.cmfObject.getId()),
						e);
				}
			}
		}

		try {
			applyAttributes(targetFile, translator);
		} catch (Exception e) {
			throw new ImportException(
				String.format("Failed to apply attributes to the target file [%s] for %s [%s](%s)", targetFile,
					this.cmfObject.getType(), this.cmfObject.getLabel(), this.cmfObject.getId()),
				e);
		}
		return Collections.singleton(new ImportOutcome(created ? ImportResult.CREATED : ImportResult.UPDATED,
			getNewId(targetFile), targetFile.getAbsolutePath()));
	}
}