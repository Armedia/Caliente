package com.armedia.cmf.engine.alfresco.bulk.importer;

import com.armedia.cmf.engine.alfresco.bulk.importer.jaxb.UserT;
import com.armedia.cmf.engine.alfresco.bulk.importer.jaxb.UsersT;
import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class AlfUserImportDelegate extends AlfAggregatedImportDelegate<UserT, UsersT> {

	protected AlfUserImportDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, UsersT.class);
	}

	@Override
	protected UserT createItem(CmfAttributeTranslator<CmfValue> translator, AlfImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
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