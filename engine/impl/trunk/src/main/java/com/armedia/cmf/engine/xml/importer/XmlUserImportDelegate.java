package com.armedia.cmf.engine.xml.importer;

import java.util.Collection;
import java.util.Collections;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.engine.xml.importer.jaxb.UserT;
import com.armedia.cmf.engine.xml.importer.jaxb.UsersT;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class XmlUserImportDelegate extends XmlSharedFileImportDelegate<UsersT> {

	protected XmlUserImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, UsersT.class);
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
		UserT user = new UserT();

		user.setName(getAttributeValue(IntermediateAttribute.NAME.encode()).asString());
		user.setLoginDomain(getAttributeValue(IntermediateAttribute.LOGIN_REALM.encode()).asString());
		user.setLoginName(getAttributeValue(IntermediateAttribute.LOGIN_NAME.encode()).asString());
		user.setOsDomain(getAttributeValue(IntermediateAttribute.OS_NAME.encode()).asString());
		user.setOsName(getAttributeValue(IntermediateAttribute.OS_NAME.encode()).asString());
		user.setDescription(getAttributeValue(IntermediateAttribute.DESCRIPTION.encode()).asString());
		user.setEmail(getAttributeValue(IntermediateAttribute.EMAIL.encode()).asString());
		user.setDefaultFolder(getAttributeValue(IntermediateAttribute.DEFAULT_FOLDER.encode()).asString());

		getXmlObject().getUsers().add(user);
		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), this.cmfObject
			.getLabel()));
	}
}