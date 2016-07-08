package com.armedia.cmf.engine.alfresco.bulk.importer;

import com.armedia.cmf.engine.alfresco.bulk.importer.jaxb.FormatT;
import com.armedia.cmf.engine.alfresco.bulk.importer.jaxb.FormatsT;
import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class AlfFormatImportDelegate extends AlfAggregatedImportDelegate<FormatT, FormatsT> {

	protected AlfFormatImportDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, FormatsT.class);
	}

	@Override
	protected FormatT createItem(CmfAttributeTranslator<CmfValue> translator, AlfImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
		FormatT format = new FormatT();

		format.setName(getAttributeValue(IntermediateAttribute.NAME).asString());
		format.setDescription(getAttributeValue(IntermediateAttribute.DESCRIPTION).asString());

		dumpAttributes(format.getAttributes());
		dumpProperties(format.getProperties());
		return format;
	}
}