package com.armedia.cmf.engine.xml.importer;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.xml.importer.jaxb.DocumentIndexEntryT;
import com.armedia.cmf.engine.xml.importer.jaxb.DocumentIndexT;
import com.armedia.cmf.engine.xml.importer.jaxb.DocumentT;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class XmlDocumentImportDelegate extends XmlAggregatedImportDelegate<DocumentIndexEntryT, DocumentIndexT> {

	private final XmlAggregateDocumentsImportDelegate delegate;

	protected XmlDocumentImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, DocumentIndexT.class);
		this.delegate = new XmlAggregateDocumentsImportDelegate(factory, storedObject);
	}

	@Override
	protected DocumentIndexEntryT createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {

		DocumentT d = this.delegate.createItem(translator, ctx, true);
		if (d == null) { return null; }

		return null;
	}
}