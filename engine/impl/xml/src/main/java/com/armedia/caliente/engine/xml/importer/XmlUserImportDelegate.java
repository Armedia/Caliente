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
package com.armedia.caliente.engine.xml.importer;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.xml.importer.jaxb.UserT;
import com.armedia.caliente.engine.xml.importer.jaxb.UsersT;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public class XmlUserImportDelegate extends XmlAggregatedImportDelegate<UserT, UsersT> {

	protected XmlUserImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, UsersT.class);
	}

	@Override
	protected UserT createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException {
		UserT user = new UserT();

		user.setName(getAttributeValue(IntermediateAttribute.NAME).asString());
		user.setSource(getAttributeValue(IntermediateAttribute.USER_SOURCE).asString());
		user.setLoginDomain(getAttributeValue(IntermediateAttribute.LOGIN_REALM).asString());
		user.setLoginName(getAttributeValue(IntermediateAttribute.LOGIN_NAME).asString());
		user.setOsDomain(getAttributeValue(IntermediateAttribute.OS_REALM).asString());
		user.setOsName(getAttributeValue(IntermediateAttribute.OS_NAME).asString());
		user.setDescription(getAttributeValue(IntermediateAttribute.DESCRIPTION).asString());
		user.setEmail(getAttributeValue(IntermediateAttribute.EMAIL).asString());
		user.setDefaultFolder(getAttributeValue(IntermediateAttribute.DEFAULT_FOLDER).asString());

		dumpAttributes(user.getAttributes());
		dumpProperties(user.getProperties());
		return user;
	}
}