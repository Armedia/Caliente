package com.armedia.cmf.engine.xml.importer;

import java.util.Collection;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.xml.importer.jaxb.AclsT;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class XmlAclImportDelegate extends XmlSharedFileImportDelegate<AclsT> {

	protected XmlAclImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, storedObject, AclsT.class);
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
		return null;
	}
}