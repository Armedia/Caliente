package com.armedia.cmf.engine.local.common;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.cmf.storage.StoredValueCodec;
import com.armedia.commons.utilities.Tools;

public class LocalTranslator extends ObjectStorageTranslator<StoredValue> {

	private static final Map<PropertyType, StoredDataType> DATA_TYPES;
	private static final Map<StoredDataType, PropertyType> DATA_TYPES_REV;

	private static final Map<StoredObjectType, BidiMap<String, IntermediateAttribute>> ATTRIBUTE_MAPPINGS;

	static {
		Map<PropertyType, StoredDataType> m = new EnumMap<PropertyType, StoredDataType>(PropertyType.class);
		m.put(PropertyType.BOOLEAN, StoredDataType.BOOLEAN);
		m.put(PropertyType.INTEGER, StoredDataType.INTEGER);
		m.put(PropertyType.DECIMAL, StoredDataType.DOUBLE);
		m.put(PropertyType.DATETIME, StoredDataType.DATETIME);
		m.put(PropertyType.ID, StoredDataType.ID);
		m.put(PropertyType.STRING, StoredDataType.STRING);
		m.put(PropertyType.URI, StoredDataType.STRING); // TODO: Add this to StoredDataType
		m.put(PropertyType.HTML, StoredDataType.STRING); // TODO: Add this to StoredDataType
		DATA_TYPES = Tools.freezeMap(m);

		Map<StoredDataType, PropertyType> n = new EnumMap<StoredDataType, PropertyType>(StoredDataType.class);
		n.put(StoredDataType.BOOLEAN, PropertyType.BOOLEAN);
		n.put(StoredDataType.INTEGER, PropertyType.INTEGER);
		n.put(StoredDataType.DOUBLE, PropertyType.DECIMAL);
		n.put(StoredDataType.DATETIME, PropertyType.DATETIME);
		n.put(StoredDataType.ID, PropertyType.ID);
		n.put(StoredDataType.STRING, PropertyType.STRING); // TODO: Need to handle HTML and URI
		DATA_TYPES_REV = Tools.freezeMap(n);

		Map<StoredObjectType, BidiMap<String, IntermediateAttribute>> attributeMappings = new EnumMap<StoredObjectType, BidiMap<String, IntermediateAttribute>>(
			StoredObjectType.class);

		BidiMap<String, IntermediateAttribute> am = null;

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		// BASE_TYPE_ID (ACL)
		// OBJECT_TYPE_ID (DM_ACL)
		// am.put(LocalAttributes.ACL_OWNER.name, IntermediateAttribute.OWNER);
		attributeMappings.put(StoredObjectType.ACL, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		// BASE_TYPE_ID (DOCUMENT)
		// OBJECT_TYPE_ID (cmis:document|...)
		// am.put(LocalAttributes.VERSION_ANTECEDENT_ID.name,
		// IntermediateAttribute.VERSION_ANTECEDENT_ID);
		attributeMappings.put(StoredObjectType.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		ATTRIBUTE_MAPPINGS = Tools.freezeMap(attributeMappings);
	}

	public static StoredDataType decodePropertyType(PropertyType t) {
		return LocalTranslator.DATA_TYPES.get(t);
	}

	public static PropertyType decodePropertyType(StoredObjectType t) {
		return LocalTranslator.DATA_TYPES_REV.get(t);
	}

	@Override
	public StoredObject<StoredValue> decodeObject(StoredObject<StoredValue> rawObject) {
		return super.decodeObject(rawObject);
	}

	@Override
	public StoredObject<StoredValue> encodeObject(StoredObject<StoredValue> rawObject) {
		return super.encodeObject(rawObject);
	}

	private BidiMap<String, IntermediateAttribute> getAttributeMappings(StoredObjectType type) {
		return LocalTranslator.ATTRIBUTE_MAPPINGS.get(type);
	}

	@Override
	public String encodeAttributeName(StoredObjectType type, String attributeName) {
		BidiMap<String, IntermediateAttribute> mappings = getAttributeMappings(type);
		if (mappings != null) {
			// TODO: normalize the CMS attribute name
			IntermediateAttribute att = mappings.get(attributeName);
			if (att != null) { return att.encode(); }
		}
		return super.encodeAttributeName(type, attributeName);
	}

	@Override
	public String decodeAttributeName(StoredObjectType type, String attributeName) {
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
	public StoredValueCodec<StoredValue> getCodec(StoredDataType type) {
		return ObjectStorageTranslator.getStoredValueCodec(type);
	}

	@Override
	public StoredValue getValue(StoredDataType type, Object value) throws ParseException {
		return new StoredValue(type, value);
	}
}