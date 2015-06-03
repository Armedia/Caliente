package com.armedia.cmf.engine.cmis.importer;

import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.importer.ImportDelegate;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public abstract class CmisImportDelegate<T>
	extends
	ImportDelegate<T, Session, CmisSessionWrapper, CmfValue, CmisImportContext, CmisImportDelegateFactory, CmisImportEngine> {

	protected CmisImportDelegate(CmisImportDelegateFactory factory, Class<T> objectClass,
		CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, objectClass, storedObject);
	}

	@Override
	protected ImportOutcome importObject(CmfAttributeTranslator<CmfValue> translator, CmisImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
		return null;
	}
}