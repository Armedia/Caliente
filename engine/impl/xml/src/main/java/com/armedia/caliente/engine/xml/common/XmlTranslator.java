package com.armedia.caliente.engine.xml.common;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.store.CmfAttributeNameMapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfValueType;
import com.armedia.caliente.store.CmfArchetype;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

public class XmlTranslator extends CmfAttributeTranslator<CmfValue> {

	private static final Map<PropertyType, CmfValueType> DATA_TYPES;
	private static final Map<CmfValueType, PropertyType> DATA_TYPES_REV;

	private static final Map<CmfArchetype, BidiMap<String, IntermediateAttribute>> ATTRIBUTE_MAPPINGS;

	static {
		Map<PropertyType, CmfValueType> m = new EnumMap<>(PropertyType.class);
		m.put(PropertyType.BOOLEAN, CmfValueType.BOOLEAN);
		m.put(PropertyType.INTEGER, CmfValueType.INTEGER);
		m.put(PropertyType.DECIMAL, CmfValueType.DOUBLE);
		m.put(PropertyType.DATETIME, CmfValueType.DATETIME);
		m.put(PropertyType.ID, CmfValueType.ID);
		m.put(PropertyType.STRING, CmfValueType.STRING);
		m.put(PropertyType.URI, CmfValueType.STRING); // TODO: Add this to CmfDataType
		m.put(PropertyType.HTML, CmfValueType.STRING); // TODO: Add this to CmfDataType
		DATA_TYPES = Tools.freezeMap(m);

		Map<CmfValueType, PropertyType> n = new EnumMap<>(CmfValueType.class);
		n.put(CmfValueType.BOOLEAN, PropertyType.BOOLEAN);
		n.put(CmfValueType.INTEGER, PropertyType.INTEGER);
		n.put(CmfValueType.DOUBLE, PropertyType.DECIMAL);
		n.put(CmfValueType.DATETIME, PropertyType.DATETIME);
		n.put(CmfValueType.ID, PropertyType.ID);
		n.put(CmfValueType.STRING, PropertyType.STRING); // TODO: Need to handle HTML and URI
		DATA_TYPES_REV = Tools.freezeMap(n);

		Map<CmfArchetype, BidiMap<String, IntermediateAttribute>> attributeMappings = new EnumMap<>(CmfArchetype.class);

		BidiMap<String, IntermediateAttribute> am = null;

		am = new DualHashBidiMap<>();
		// BASE_TYPE_ID (DOCUMENT)
		// OBJECT_TYPE_ID (cmis:document|...)
		// am.put(XmlAttributes.VERSION_ANTECEDENT_ID.name,
		// IntermediateAttribute.VERSION_ANTECEDENT_ID);
		attributeMappings.put(CmfArchetype.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		ATTRIBUTE_MAPPINGS = Tools.freezeMap(attributeMappings);
	}

	public static CmfValueType decodePropertyType(PropertyType t) {
		return XmlTranslator.DATA_TYPES.get(t);
	}

	public static PropertyType decodePropertyType(CmfValueType t) {
		return XmlTranslator.DATA_TYPES_REV.get(t);
	}

	private static BidiMap<String, IntermediateAttribute> getAttributeMappings(CmfArchetype type) {
		return XmlTranslator.ATTRIBUTE_MAPPINGS.get(type);
	}

	@SuppressWarnings("unused")
	private static final CmfAttributeNameMapper MAPPER = new CmfAttributeNameMapper() {

		@Override
		public String encodeAttributeName(CmfArchetype type, String attributeName) {
			BidiMap<String, IntermediateAttribute> mappings = XmlTranslator.getAttributeMappings(type);
			if (mappings != null) {
				// TODO: normalize the CMS attribute name
				IntermediateAttribute att = mappings.get(attributeName);
				if (att != null) { return att.encode(); }
			}
			return super.encodeAttributeName(type, attributeName);
		}

		@Override
		public String decodeAttributeName(CmfArchetype type, String attributeName) {
			BidiMap<String, IntermediateAttribute> mappings = XmlTranslator.getAttributeMappings(type);
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
	};

	public XmlTranslator() {
		super(CmfValue.class, null);
	}

	@Override
	public CmfValueCodec<CmfValue> getCodec(CmfValueType type) {
		return CmfAttributeTranslator.getStoredValueCodec(type);
	}

	@Override
	public CmfValue getValue(CmfValueType type, Object value) throws ParseException {
		return new CmfValue(type, value);
	}
}