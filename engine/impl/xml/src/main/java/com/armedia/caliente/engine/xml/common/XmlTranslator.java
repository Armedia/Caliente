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
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

public class XmlTranslator extends CmfAttributeTranslator<CmfValue> {

	private static final Map<PropertyType, CmfValue.Type> DATA_TYPES;
	private static final Map<CmfValue.Type, PropertyType> DATA_TYPES_REV;

	private static final Map<CmfObject.Archetype, BidiMap<String, IntermediateAttribute>> ATTRIBUTE_MAPPINGS;

	static {
		Map<PropertyType, CmfValue.Type> m = new EnumMap<>(PropertyType.class);
		m.put(PropertyType.BOOLEAN, CmfValue.Type.BOOLEAN);
		m.put(PropertyType.INTEGER, CmfValue.Type.INTEGER);
		m.put(PropertyType.DECIMAL, CmfValue.Type.DOUBLE);
		m.put(PropertyType.DATETIME, CmfValue.Type.DATETIME);
		m.put(PropertyType.ID, CmfValue.Type.ID);
		m.put(PropertyType.STRING, CmfValue.Type.STRING);
		m.put(PropertyType.URI, CmfValue.Type.STRING); // TODO: Add this to CmfValue.Type
		m.put(PropertyType.HTML, CmfValue.Type.STRING); // TODO: Add this to CmfValue.Type
		DATA_TYPES = Tools.freezeMap(m);

		Map<CmfValue.Type, PropertyType> n = new EnumMap<>(CmfValue.Type.class);
		n.put(CmfValue.Type.BOOLEAN, PropertyType.BOOLEAN);
		n.put(CmfValue.Type.INTEGER, PropertyType.INTEGER);
		n.put(CmfValue.Type.DOUBLE, PropertyType.DECIMAL);
		n.put(CmfValue.Type.DATETIME, PropertyType.DATETIME);
		n.put(CmfValue.Type.ID, PropertyType.ID);
		n.put(CmfValue.Type.STRING, PropertyType.STRING); // TODO: Need to handle HTML and URI
		DATA_TYPES_REV = Tools.freezeMap(n);

		Map<CmfObject.Archetype, BidiMap<String, IntermediateAttribute>> attributeMappings = new EnumMap<>(CmfObject.Archetype.class);

		BidiMap<String, IntermediateAttribute> am = null;

		am = new DualHashBidiMap<>();
		// BASE_TYPE_ID (DOCUMENT)
		// OBJECT_TYPE_ID (cmis:document|...)
		// am.put(XmlAttributes.VERSION_ANTECEDENT_ID.name,
		// IntermediateAttribute.VERSION_ANTECEDENT_ID);
		attributeMappings.put(CmfObject.Archetype.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		ATTRIBUTE_MAPPINGS = Tools.freezeMap(attributeMappings);
	}

	public static CmfValue.Type decodePropertyType(PropertyType t) {
		return XmlTranslator.DATA_TYPES.get(t);
	}

	public static PropertyType decodePropertyType(CmfValue.Type t) {
		return XmlTranslator.DATA_TYPES_REV.get(t);
	}

	private static BidiMap<String, IntermediateAttribute> getAttributeMappings(CmfObject.Archetype type) {
		return XmlTranslator.ATTRIBUTE_MAPPINGS.get(type);
	}

	@SuppressWarnings("unused")
	private static final CmfAttributeNameMapper MAPPER = new CmfAttributeNameMapper() {

		@Override
		public String encodeAttributeName(CmfObject.Archetype type, String attributeName) {
			BidiMap<String, IntermediateAttribute> mappings = XmlTranslator.getAttributeMappings(type);
			if (mappings != null) {
				// TODO: normalize the CMS attribute name
				IntermediateAttribute att = mappings.get(attributeName);
				if (att != null) { return att.encode(); }
			}
			return super.encodeAttributeName(type, attributeName);
		}

		@Override
		public String decodeAttributeName(CmfObject.Archetype type, String attributeName) {
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
	public CmfValueCodec<CmfValue> getCodec(CmfValue.Type type) {
		return CmfAttributeTranslator.getStoredValueCodec(type);
	}

	@Override
	public CmfValue getValue(CmfValue.Type type, Object value) throws ParseException {
		return new CmfValue(type, value);
	}
}