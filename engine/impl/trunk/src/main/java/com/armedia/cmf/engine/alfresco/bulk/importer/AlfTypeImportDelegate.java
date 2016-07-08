package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.alfresco.bulk.importer.jaxb.AttributeDefT;
import com.armedia.cmf.engine.alfresco.bulk.importer.jaxb.DataTypeT;
import com.armedia.cmf.engine.alfresco.bulk.importer.jaxb.TypeT;
import com.armedia.cmf.engine.alfresco.bulk.importer.jaxb.TypesT;
import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;
import com.armedia.commons.utilities.Tools;

public class AlfTypeImportDelegate extends AlfAggregatedImportDelegate<TypeT, TypesT> {

	protected AlfTypeImportDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, TypesT.class);
	}

	@Override
	protected TypeT createItem(CmfAttributeTranslator<CmfValue> translator, AlfImportContext ctx)
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

		CmfProperty<CmfValue> origName = this.cmfObject.getProperty(IntermediateProperty.ORIG_ATTR_NAME);
		CmfProperty<CmfValue> mappedName = this.cmfObject.getProperty(IntermediateProperty.MAPPED_ATTR_NAME);

		Map<String, String> mapping = new HashMap<String, String>();
		// If the mappings are in order, use them...
		if ((origName != null) && (mappedName != null) && (origName.getValueCount() == mappedName.getValueCount())) {
			for (int i = 0; i < origName.getValueCount(); i++) {
				mapping.put(origName.getValue(i).asString(), mappedName.getValue(i).asString());
			}
		}

		for (int i = 0; i < attrCount; ++i) {
			boolean inherited = (i < startPosition);
			AttributeDefT def = new AttributeDefT();
			final String srcName = attName.getValue(i).asString();
			final String tgtName = Tools.coalesce(mapping.get(srcName), srcName);
			def.setName(tgtName);
			def.setSourceName(srcName);
			DataTypeT dt = null;
			// We convert from the documentum values, because we can't do it more cleanly
			switch (attType.getValue(i).asInteger()) {
				case 0:
					dt = DataTypeT.BOOLEAN;
					break;
				case 1:
					dt = DataTypeT.INTEGER;
					break;
				case 2:
					dt = DataTypeT.STRING;
					break;
				case 3:
					dt = DataTypeT.ID;
					break;
				case 4:
					dt = DataTypeT.DATETIME;
					break;
				case 5:
					dt = DataTypeT.DOUBLE;
					break;

			}
			def.setDataType(dt);
			def.setInherited(inherited);
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