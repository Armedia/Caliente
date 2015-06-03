package com.armedia.cmf.engine.cmis.importer;

import org.apache.chemistry.opencmis.client.api.Document;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class CmisImportDelegateDocument extends CmisImportDelegate<Document> {

	public CmisImportDelegateDocument(CmisImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, Document.class, storedObject);
	}

	@Override
	protected ImportOutcome importObject(CmfAttributeTranslator<CmfValue> translator, CmisImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
		return super.importObject(translator, ctx);
	}
}