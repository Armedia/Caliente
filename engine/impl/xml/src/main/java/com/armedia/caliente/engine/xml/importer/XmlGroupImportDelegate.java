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
package com.armedia.caliente.engine.xml.importer;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.xml.importer.jaxb.GroupT;
import com.armedia.caliente.engine.xml.importer.jaxb.GroupsT;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public class XmlGroupImportDelegate extends XmlAggregatedImportDelegate<GroupT, GroupsT> {

	protected XmlGroupImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, GroupsT.class);
	}

	@Override
	protected GroupT createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException {
		GroupT group = new GroupT();

		group.setName(getAttributeValue(IntermediateAttribute.NAME).asString());
		group.setAdministrator(getAttributeValue(IntermediateAttribute.ADMINISTRATOR).asString());
		group.setDisplayName(getAttributeValue(IntermediateAttribute.ADMINISTRATOR).asString());
		group.setEmail(getAttributeValue(IntermediateAttribute.EMAIL).asString());
		group.setSource(getAttributeValue(IntermediateAttribute.GROUP_SOURCE).asString());
		group.setType(getAttributeValue(IntermediateAttribute.GROUP_TYPE).asString());

		for (CmfValue v : getAttributeValues("dctm:users_names")) {
			group.addUser(v.asString());
		}

		for (CmfValue v : getAttributeValues("dctm:groups_names")) {
			group.addGroup(v.asString());
		}

		dumpAttributes(group.getAttributes());
		dumpProperties(group.getProperties());
		return group;
	}
}