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
package com.armedia.caliente.engine.alfresco.bi.importer;

import java.util.Collection;
import java.util.Collections;

import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public class AlfImportUserDelegate extends AlfImportDelegate {

	public AlfImportUserDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, storedObject);
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, AlfImportContext ctx)
		throws ImportException, CmfStorageException {

		// Special dispensation for Documentum...
		CmfValue group = getAttributeValue("dctm:r_is_group");
		if ((group != null) && group.asBoolean()) { return Collections.singleton(ImportOutcome.SKIPPED); }

		CmfValue name = getAttributeValue("cmis:name");
		if (name == null) { return Collections.singleton(ImportOutcome.SKIPPED); }
		CmfValue login = getAttributeValue("cmf:login_name");
		if (login == null) { return Collections.singleton(ImportOutcome.SKIPPED); }

		if (this.factory.mapUserLogin(name.asString(), login.asString())) {
			ctx.printf("Mapped username [%s] to loginname [%s]...", name.asString(), login.asString());
		}
		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, login.asString(), login.asString()));
	}
}