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