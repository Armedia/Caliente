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
/**
 *
 */

package com.armedia.caliente.engine.dfc.importer;

import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.store.CmfObject;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 *
 *
 */
public class DctmImportFormat extends DctmImportDelegate<IDfFormat> {

	public DctmImportFormat(DctmImportDelegateFactory factory, CmfObject<IDfValue> storedObject) throws Exception {
		super(factory, IDfFormat.class, DctmObjectType.FORMAT, storedObject);
	}

	@Override
	protected String calculateLabel(IDfFormat format) throws DfException {
		return format.getName();
	}

	@Override
	protected void finalizeConstruction(IDfFormat object, boolean newObject, DctmImportContext context)
		throws DfException {
		if (newObject) {
			copyAttributeToObject(DctmAttributes.NAME, object);
		}
	}

	@Override
	protected String generateSystemAttributesSQL(CmfObject<IDfValue> stored, IDfPersistentObject object,
		DctmImportContext ctx) throws DfException {
		return null;
	}

	@Override
	protected IDfFormat locateInCms(DctmImportContext ctx) throws DfException {
		IDfSession session = ctx.getSession();
		IDfValue formatName = this.cmfObject.getAttribute(DctmAttributes.NAME).getValue();
		return session.getFormat(formatName.asString());
	}
}