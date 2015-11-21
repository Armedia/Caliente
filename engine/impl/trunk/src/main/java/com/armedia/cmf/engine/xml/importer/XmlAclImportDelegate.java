package com.armedia.cmf.engine.xml.importer;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.xml.importer.jaxb.AclPermitT;
import com.armedia.cmf.engine.xml.importer.jaxb.AclT;
import com.armedia.cmf.engine.xml.importer.jaxb.AclsT;
import com.armedia.cmf.engine.xml.importer.jaxb.PermitTypeT;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;
import com.armedia.commons.utilities.Tools;

public class XmlAclImportDelegate extends XmlAggregatedImportDelegate<AclT, AclsT> {

	private static final Map<Integer, String> XPERMITS;

	static {
		Map<Integer, String> m = new HashMap<Integer, String>();
		m.put(19, "DELETE_OBJECT");
		m.put(18, "CHANGE_OWNER");
		m.put(17, "CHANGE_PERMIT");
		m.put(16, "CHANGE_STATE");
		m.put(1, "CHANGE_LOCATION");
		m.put(0, "EXECUTE_PROC");
		XPERMITS = Tools.freezeMap(m);
	}

	protected XmlAclImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, storedObject, AclsT.class);
	}

	@Override
	protected AclT createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
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
		return acl;
	}
}