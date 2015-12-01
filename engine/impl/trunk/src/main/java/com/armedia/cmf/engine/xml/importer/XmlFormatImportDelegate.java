package com.armedia.cmf.engine.xml.importer;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.xml.importer.jaxb.FormatT;
import com.armedia.cmf.engine.xml.importer.jaxb.FormatsT;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class XmlFormatImportDelegate extends XmlAggregatedImportDelegate<FormatT, FormatsT> {

	protected XmlFormatImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, FormatsT.class);
	}

	@Override
	protected FormatT createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
		FormatT format = new FormatT();

		format.setName(getAttributeValue(IntermediateAttribute.NAME).asString());
		format.setDescription(getAttributeValue(IntermediateAttribute.DESCRIPTION).asString());

		dumpAttributes(format.getAttributes());
		dumpProperties(format.getProperties());
		return format;
	}
}