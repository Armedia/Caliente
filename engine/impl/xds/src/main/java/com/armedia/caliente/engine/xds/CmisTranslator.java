package com.armedia.caliente.engine.xds;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.store.CmfAttributeNameMapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

public class CmisTranslator extends CmfAttributeTranslator<CmfValue> {

	private static final Map<PropertyType, CmfDataType> DATA_TYPES;
	private static final Map<CmfDataType, PropertyType> DATA_TYPES_REV;

	private static final Map<BaseTypeId, CmfType> OBJECT_TYPES;
	private static final Map<CmfType, BaseTypeId> OBJECT_TYPES_REV;

	private static final Map<CmfType, BidiMap<String, IntermediateAttribute>> ATTRIBUTE_MAPPINGS;

	static {
		Map<PropertyType, CmfDataType> m = new EnumMap<>(PropertyType.class);
		m.put(PropertyType.BOOLEAN, CmfDataType.BOOLEAN);
		m.put(PropertyType.INTEGER, CmfDataType.INTEGER);
		m.put(PropertyType.DECIMAL, CmfDataType.DOUBLE);
		m.put(PropertyType.DATETIME, CmfDataType.DATETIME);
		m.put(PropertyType.ID, CmfDataType.ID);
		m.put(PropertyType.STRING, CmfDataType.STRING);
		m.put(PropertyType.URI, CmfDataType.URI);
		m.put(PropertyType.HTML, CmfDataType.HTML);
		DATA_TYPES = Tools.freezeMap(m);

		Map<CmfDataType, PropertyType> n = new EnumMap<>(CmfDataType.class);
		n.put(CmfDataType.BOOLEAN, PropertyType.BOOLEAN);
		n.put(CmfDataType.INTEGER, PropertyType.INTEGER);
		n.put(CmfDataType.DOUBLE, PropertyType.DECIMAL);
		n.put(CmfDataType.DATETIME, PropertyType.DATETIME);
		n.put(CmfDataType.ID, PropertyType.ID);
		n.put(CmfDataType.STRING, PropertyType.STRING);
		n.put(CmfDataType.URI, PropertyType.URI);
		n.put(CmfDataType.HTML, PropertyType.HTML);
		DATA_TYPES_REV = Tools.freezeMap(n);

		Map<BaseTypeId, CmfType> o = new EnumMap<>(BaseTypeId.class);
		o.put(BaseTypeId.CMIS_DOCUMENT, CmfType.DOCUMENT);
		o.put(BaseTypeId.CMIS_FOLDER, CmfType.FOLDER);
		// TODO: add other types...such as policies
		OBJECT_TYPES = Tools.freezeMap(o);

		Map<CmfType, BaseTypeId> p = new EnumMap<>(CmfType.class);
		p.put(CmfType.DOCUMENT, BaseTypeId.CMIS_DOCUMENT);
		p.put(CmfType.FOLDER, BaseTypeId.CMIS_FOLDER);
		// TODO: add other types...such as policies
		OBJECT_TYPES_REV = Tools.freezeMap(p);

		Map<CmfType, BidiMap<String, IntermediateAttribute>> attributeMappings = new EnumMap<>(CmfType.class);

		BidiMap<String, IntermediateAttribute> am = null;
		am = new DualHashBidiMap<>();
		// BASE_TYPE_ID (DOCUMENT)
		// OBJECT_TYPE_ID (cmis:document|...)
		am.put(CmisCustomAttributes.VERSION_ANTECEDENT_ID.name, IntermediateAttribute.VERSION_ANTECEDENT_ID);
		attributeMappings.put(CmfType.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		ATTRIBUTE_MAPPINGS = Tools.freezeMap(attributeMappings);
	}

	public static CmfDataType decodePropertyType(PropertyType t) {
		return CmisTranslator.DATA_TYPES.get(t);
	}

	public static PropertyType decodePropertyType(CmfDataType t) {
		return CmisTranslator.DATA_TYPES_REV.get(t);
	}

	public static CmfType decodeObjectType(BaseTypeId type) {
		return CmisTranslator.OBJECT_TYPES.get(type);
	}

	public static BaseTypeId decodeObjectType(CmfType type) {
		return CmisTranslator.OBJECT_TYPES_REV.get(type);
	}

	private static final CmfAttributeNameMapper MAPPER = new CmfAttributeNameMapper() {
		@Override
		public String encodeAttributeName(CmfType type, String attributeName) {
			BidiMap<String, IntermediateAttribute> mappings = CmisTranslator.getAttributeMappings(type);
			if (mappings != null) {
				// TODO: normalize the CMS attribute name
				IntermediateAttribute att = mappings.get(attributeName);
				if (att != null) { return att.encode(); }
			}
			return super.encodeAttributeName(type, attributeName);
		}

		@Override
		public String decodeAttributeName(CmfType type, String attributeName) {
			BidiMap<String, IntermediateAttribute> mappings = CmisTranslator.getAttributeMappings(type);
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

	public CmisTranslator() {
		super(CmfValue.class, CmisTranslator.MAPPER);
	}

	private static BidiMap<String, IntermediateAttribute> getAttributeMappings(CmfType type) {
		return CmisTranslator.ATTRIBUTE_MAPPINGS.get(type);
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