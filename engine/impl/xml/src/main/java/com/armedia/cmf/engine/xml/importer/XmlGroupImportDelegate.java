package com.armedia.cmf.engine.xml.importer;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.xml.importer.jaxb.GroupT;
import com.armedia.cmf.engine.xml.importer.jaxb.GroupsT;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;

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