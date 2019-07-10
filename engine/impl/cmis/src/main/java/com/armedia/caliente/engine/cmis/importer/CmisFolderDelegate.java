/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.engine.cmis.importer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public class CmisFolderDelegate extends CmisFileableDelegate<Folder> {

	public CmisFolderDelegate(CmisImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, Folder.class, storedObject);
	}

	@Override
	protected Folder createNew(CmisImportContext ctx, Folder parent, Map<String, Object> properties)
		throws ImportException {
		return parent.createFolder(properties);
	}

	@Override
	protected boolean isMultifilable(Folder existing) {
		return false;
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, CmisImportContext ctx)
		throws ImportException, CmfStorageException {

		Map<String, Object> props = super.prepareProperties(translator, ctx);
		props.remove(PropertyIds.PATH);
		props.remove(PropertyIds.PARENT_ID);

		List<Folder> parents = getParentFolders(ctx);
		List<ImportOutcome> outcomes = new ArrayList<>(parents.size());
		CmfAttribute<CmfValue> nameAtt = this.cmfObject.getAttribute(PropertyIds.NAME);
		if ((nameAtt == null) || !nameAtt.hasValues()) {
			throw new ImportException(
				String.format("No %s attribute found for %s", PropertyIds.NAME, this.cmfObject.getDescription()));
		}
		final CmfValue nameValue = nameAtt.getValue();
		if (nameValue.isNull()) {
			throw new ImportException(String.format("%s attribute has a null value for %s", PropertyIds.NAME,
				this.cmfObject.getDescription()));
		}
		final String name = nameValue.asString();
		for (Folder f : getParentFolders(ctx)) {
			String path = String.format("%s/%s", f.getPath(), name);
			Folder existing = null;
			try {
				FileableCmisObject obj = FileableCmisObject.class.cast(ctx.getSession().getObjectByPath(path));
				if (obj instanceof Folder) {
					existing = Folder.class.cast(obj);
				}
			} catch (CmisObjectNotFoundException e) {
				existing = null;
			}

			if (existing == null) {
				// If it doesn't exist, we'll create the new object...
				existing = createNew(ctx, f, props);
				applyAcl(ctx, existing);
				outcomes.add(new ImportOutcome(ImportResult.CREATED, existing.getId(), calculateNewLabel(existing)));
				continue;
			}

			if (isSameObject(existing)) {
				outcomes.add(new ImportOutcome(ImportResult.DUPLICATE, existing.getId(), calculateNewLabel(existing)));
				continue;
			}

			// Not the same...we must update the properties and/or content
			updateExisting(ctx, existing, props);
			applyAcl(ctx, existing);
			outcomes.add(new ImportOutcome(ImportResult.UPDATED, existing.getId(), calculateNewLabel(existing)));
		}
		return outcomes;
	}
}