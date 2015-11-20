package com.armedia.cmf.engine.xml.importer;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.xml.importer.jaxb.AttributeDefT;
import com.armedia.cmf.engine.xml.importer.jaxb.DataTypeT;
import com.armedia.cmf.engine.xml.importer.jaxb.TypeT;
import com.armedia.cmf.engine.xml.importer.jaxb.TypesT;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class XmlTypeImportDelegate extends XmlAggregatedImportDelegate<TypeT, TypesT> {

	protected XmlTypeImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, TypesT.class);
	}

	@Override
	protected TypeT createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
		TypeT type = new TypeT();

		type.setName(getAttributeValue(IntermediateAttribute.NAME).asString());

		String superType = getAttributeValue(IntermediateAttribute.SUPER_NAME).asString();
		if (!StringUtils.isBlank(superType)) {
			type.setSuperType(superType);
		} else {
			type.setSuperType(null);
		}

		final int attrCount = getAttributeValue("dctm:attr_count").asInteger();
		final int startPosition = getAttributeValue("dctm:start_pos").asInteger();
		CmfAttribute<CmfValue> attName = this.cmfObject.getAttribute("dctm:attr_name");
		CmfAttribute<CmfValue> attType = this.cmfObject.getAttribute("dctm:attr_type");
		CmfAttribute<CmfValue> attRep = this.cmfObject.getAttribute("dctm:attr_repeating");
		CmfAttribute<CmfValue> attLen = this.cmfObject.getAttribute("dctm:attr_length");

		for (int i = startPosition; i < attrCount; ++i) {
			AttributeDefT def = new AttributeDefT();
			def.setName(attName.getValue(i).asString());
			def.setDataType(DataTypeT.values()[attType.getValue(i).asInteger()]);
			def.setRepeating(attRep.getValue(i).asBoolean());
			int len = attLen.getValue(i).asInteger();
			if (len > 0) {
				def.setLength(len);
			}
			type.getAttributes().add(def);
		}
		return type;
	}
}