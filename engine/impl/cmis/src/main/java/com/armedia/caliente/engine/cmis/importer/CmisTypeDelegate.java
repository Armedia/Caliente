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
package com.armedia.caliente.engine.cmis.importer;

import java.util.Collection;
import java.util.Collections;

import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;

import com.armedia.caliente.engine.common.TypeDefinitionEncoder;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public class CmisTypeDelegate extends CmisImportDelegate<TypeDefinition> {

	public CmisTypeDelegate(CmisImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, TypeDefinition.class, storedObject);
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, CmisImportContext ctx)
		throws ImportException, CmfStorageException {
		TypeDefinition newType = TypeDefinitionEncoder.decode(this.cmfObject, CmfValue::asString);
		// TODO: Map/translate the type ID if necessary
		TypeDefinition existing = ctx.getSession().getTypeDefinition(newType.getId());
		if (existing != null) {
			// For now, we skip!
			return Collections
				.singleton(new ImportOutcome(ImportResult.SKIPPED, this.cmfObject.getId(), this.cmfObject.getLabel()));
		}

		// Create the new type...
		ctx.getSession().createType(newType);

		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, newType.getId(), newType.getId()));
	}
}