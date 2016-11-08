package com.armedia.caliente.engine.xml.importer;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.xml.importer.jaxb.FormatT;
import com.armedia.caliente.engine.xml.importer.jaxb.FormatsT;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public class XmlFormatImportDelegate extends XmlAggregatedImportDelegate<FormatT, FormatsT> {

	protected XmlFormatImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, FormatsT.class);
	}

	@Override
	protected FormatT createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException {
		FormatT format = new FormatT();

		format.setName(getAttributeValue(IntermediateAttribute.NAME).asString());
		format.setDescription(getAttributeValue(IntermediateAttribute.DESCRIPTION).asString());

		dumpAttributes(format.getAttributes());
		dumpProperties(format.getProperties());
		return format;
	}
}