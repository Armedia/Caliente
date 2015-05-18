package com.armedia.cmf.engine.cmis;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

public class CmisTranslator extends CmfAttributeTranslator<CmfValue> {

	private static final Map<PropertyType, CmfDataType> DATA_TYPES;
	private static final Map<CmfDataType, PropertyType> DATA_TYPES_REV;

	private static final Map<CmfType, BidiMap<String, IntermediateAttribute>> ATTRIBUTE_MAPPINGS;

	static {
		Map<PropertyType, CmfDataType> m = new EnumMap<PropertyType, CmfDataType>(PropertyType.class);
		m.put(PropertyType.BOOLEAN, CmfDataType.BOOLEAN);
		m.put(PropertyType.INTEGER, CmfDataType.INTEGER);
		m.put(PropertyType.DECIMAL, CmfDataType.DOUBLE);
		m.put(PropertyType.DATETIME, CmfDataType.DATETIME);
		m.put(PropertyType.ID, CmfDataType.ID);
		m.put(PropertyType.STRING, CmfDataType.STRING);
		m.put(PropertyType.URI, CmfDataType.STRING); // TODO: Add this to CmfDataType
		m.put(PropertyType.HTML, CmfDataType.STRING); // TODO: Add this to CmfDataType
		DATA_TYPES = Tools.freezeMap(m);

		Map<CmfDataType, PropertyType> n = new EnumMap<CmfDataType, PropertyType>(CmfDataType.class);
		n.put(CmfDataType.BOOLEAN, PropertyType.BOOLEAN);
		n.put(CmfDataType.INTEGER, PropertyType.INTEGER);
		n.put(CmfDataType.DOUBLE, PropertyType.DECIMAL);
		n.put(CmfDataType.DATETIME, PropertyType.DATETIME);
		n.put(CmfDataType.ID, PropertyType.ID);
		n.put(CmfDataType.STRING, PropertyType.STRING); // TODO: Need to handle HTML and URI
		DATA_TYPES_REV = Tools.freezeMap(n);

		Map<CmfType, BidiMap<String, IntermediateAttribute>> attributeMappings = new EnumMap<CmfType, BidiMap<String, IntermediateAttribute>>(
			CmfType.class);

		BidiMap<String, IntermediateAttribute> am = null;

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		// BASE_TYPE_ID (ACL)
		// OBJECT_TYPE_ID (DM_ACL)
		am.put(CmisCustomAttributes.ACL_OWNER.name, IntermediateAttribute.OWNER);
		attributeMappings.put(CmfType.ACL, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		// BASE_TYPE_ID (DOCUMENT)
		// OBJECT_TYPE_ID (cmis:document|...)
		am.put(CmisCustomAttributes.VERSION_ANTECEDENT_ID.name, IntermediateAttribute.VERSION_ANTECEDENT_ID);
		attributeMappings.put(CmfType.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		ATTRIBUTE_MAPPINGS = Tools.freezeMap(attributeMappings);
	}

	public static CmfDataType decodePropertyType(PropertyType t) {
		return CmisTranslator.DATA_TYPES.get(t);
	}

	public static PropertyType decodePropertyType(CmfType t) {
		return CmisTranslator.DATA_TYPES_REV.get(t);
	}

	@Override
	public CmfObject<CmfValue> decodeObject(CmfObject<CmfValue> rawObject) {
		return super.decodeObject(rawObject);
	}

	@Override
	public CmfObject<CmfValue> encodeObject(CmfObject<CmfValue> rawObject) {
		return super.encodeObject(rawObject);
	}

	private BidiMap<String, IntermediateAttribute> getAttributeMappings(CmfType type) {
		return CmisTranslator.ATTRIBUTE_MAPPINGS.get(type);
	}

	@Override
	public String encodeAttributeName(CmfType type, String attributeName) {
		BidiMap<String, IntermediateAttribute> mappings = getAttributeMappings(type);
		if (mappings != null) {
			// TODO: normalize the CMS attribute name
			IntermediateAttribute att = mappings.get(attributeName);
			if (att != null) { return att.encode(); }
		}
		return super.encodeAttributeName(type, attributeName);
	}

	@Override
	public String decodeAttributeName(CmfType type, String attributeName) {
		BidiMap<String, IntermediateAttribute> mappings = getAttributeMappings(type);
		if (mappings != null) {
			String att = null;
			try {
				// TODO: normalize the intermediate attribute name
				att = mappings.getKey(IntermediateAttribute.decode(attributeName));
			} catch (IllegalArgumentException e) {
				att = null;
			}
			if (att != null) { return att; }
		}
		return super.decodeAttributeName(type, attributeName);
	}

	@Override
	public CmfValueCodec<CmfValue> getCodec(CmfDataType type) {
		return CmfAttributeTranslator.getStoredValueCodec(type);
	}

	@Override
	public CmfValue getValue(CmfDataType type, Object value) throws ParseException {
		return new CmfValue(type, value);
	}
}