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
/**
 *
 */

package com.armedia.caliente.engine.dfc.importer;

import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.content.IDfStore;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 *
 *
 */
public class DctmImportStore extends DctmImportDelegate<IDfStore> {

	public DctmImportStore(DctmImportDelegateFactory factory, CmfObject<IDfValue> storedObject) throws Exception {
		super(factory, IDfStore.class, DctmObjectType.STORE, storedObject);
	}

	@Override
	protected IDfStore newObject(DctmImportContext ctx) throws ImportException {
		// We can't create stores programmatically....so always explode
		IDfValue name = this.cmfObject.getAttribute(DctmAttributes.NAME).getValue();
		throw new ImportException(String.format(
			"CmfStore object creation is not supported - please contact an administrator and ask them to create a store named [%s]",
			name));
	}

	@Override
	protected String calculateLabel(IDfStore store) throws DfException {
		return store.getName();
	}

	@Override
	protected IDfStore locateInCms(DctmImportContext ctx) throws DfException {
		IDfValue name = this.cmfObject.getAttribute(DctmAttributes.NAME).getValue();
		return DfcUtils.getStore(ctx.getSession(), name.asString());
	}

	@Override
	protected boolean isSameObject(IDfStore store, DctmImportContext ctx) throws DfException {
		IDfValue name = this.cmfObject.getAttribute(DctmAttributes.NAME).getValue();
		return Tools.equals(name, store.getName());
	}

	@Override
	protected String generateSystemAttributesSQL(CmfObject<IDfValue> stored, IDfPersistentObject object,
		DctmImportContext context) throws DfException {
		return null;
	}
}