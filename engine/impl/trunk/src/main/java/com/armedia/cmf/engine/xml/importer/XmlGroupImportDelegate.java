package com.armedia.cmf.engine.xml.importer;

import java.util.Collection;
import java.util.Collections;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.engine.xml.importer.jaxb.GroupT;
import com.armedia.cmf.engine.xml.importer.jaxb.GroupsT;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class XmlGroupImportDelegate extends XmlSharedFileImportDelegate<GroupsT> {

	protected XmlGroupImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, GroupsT.class);
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
		GroupT group = new GroupT();

		group.setName(getAttributeValue(IntermediateAttribute.NAME.encode()).asString());
		group.setAdministrator(getAttributeValue(IntermediateAttribute.ADMINISTRATOR.encode()).asString());
		group.setDisplayName(getAttributeValue(IntermediateAttribute.ADMINISTRATOR.encode()).asString());
		group.setEmail(getAttributeValue(IntermediateAttribute.EMAIL.encode()).asString());
		group.setType(getAttributeValue(IntermediateAttribute.GROUP_TYPE.encode()).asString());

		getXmlObject().addGroup(group);
		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), this.cmfObject
			.getLabel()));
	}
}