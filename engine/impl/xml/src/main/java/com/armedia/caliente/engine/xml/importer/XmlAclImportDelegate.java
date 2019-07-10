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

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.xml.importer.jaxb.AclPermitT;
import com.armedia.caliente.engine.xml.importer.jaxb.AclT;
import com.armedia.caliente.engine.xml.importer.jaxb.AclsT;
import com.armedia.caliente.engine.xml.importer.jaxb.PermitTypeT;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class XmlAclImportDelegate extends XmlAggregatedImportDelegate<AclT, AclsT> {

	private static final Map<Integer, String> XPERMITS;

	static {
		Map<Integer, String> m = new HashMap<>();
		m.put(19, "DELETE_OBJECT");
		m.put(18, "CHANGE_OWNER");
		m.put(17, "CHANGE_PERMIT");
		m.put(16, "CHANGE_STATE");
		m.put(1, "CHANGE_LOCATION");
		m.put(0, "EXECUTE_PROC");
		XPERMITS = Tools.freezeMap(m);
	}

	protected XmlAclImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, AclsT.class);
	}

	@Override
	protected AclT createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException {
		AclT acl = new AclT();

		acl.setId(this.cmfObject.getId());
		acl.setDescription(getAttributeValue("dctm:description").asString());

		CmfAttribute<CmfValue> accessorName = this.cmfObject.getAttribute("dctm:r_accessor_name");
		CmfAttribute<CmfValue> accessorPermit = this.cmfObject.getAttribute("dctm:r_accessor_permit");
		CmfAttribute<CmfValue> accessorXpermit = this.cmfObject.getAttribute("dctm:r_accessor_xpermit");
		CmfAttribute<CmfValue> permitType = this.cmfObject.getAttribute("dctm:r_permit_type");
		CmfAttribute<CmfValue> isGroup = this.cmfObject.getAttribute("dctm:r_is_group");

		final int entries = accessorName.getValueCount();
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < entries; i++) {
			CmfValue name = accessorName.getValue(i);
			CmfValue permit = accessorPermit.getValue(i);
			CmfValue xpermit = accessorXpermit.getValue(i);

			AclPermitT p = new AclPermitT();
			p.setType(PermitTypeT.values()[permitType.getValue(i).asInteger()]);
			p.setName(name.asString());
			p.setLevel(permit.asInteger());

			BitSet bits = new BitSet(xpermit.asInteger());
			str.setLength(0);
			for (Integer b : XmlAclImportDelegate.XPERMITS.keySet()) {
				if (bits.get(b)) {
					if (str.length() > 0) {
						str.append(',');
					}
					str.append(XmlAclImportDelegate.XPERMITS.get(b));
				}
			}
			p.setExtended(str.toString());
			(isGroup.getValue(i).asBoolean() ? acl.getGroups() : acl.getUsers()).add(p);
		}

		dumpAttributes(acl.getAttributes());
		dumpProperties(acl.getProperties());
		return acl;
	}
}