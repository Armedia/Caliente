package com.armedia.cmf.engine.xml.importer;

import java.util.Collection;
import java.util.Collections;

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
		UsersT xml = getXmlObject();
		UserT user = new UserT();

		user.setName(null);
		user.setLoginDomain(null);
		user.setLoginName(null);
		user.setOsDomain(null);
		user.setOsName(null);
		user.setDescription(null);
		user.setEmail(null);
		user.setDefaultAcl(null);
		user.setDefaultFolder(null);

		xml.getUsers().add(user);
		return Collections.singleton(new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), this.cmfObject
			.getLabel()));
	}
}