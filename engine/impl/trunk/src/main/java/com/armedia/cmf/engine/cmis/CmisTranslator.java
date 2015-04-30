package com.armedia.cmf.engine.cmis;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.cmf.storage.StoredValueCodec;
import com.armedia.commons.utilities.Tools;

public class CmisTranslator extends ObjectStorageTranslator<StoredValue> {

	private static final Map<PropertyType, StoredDataType> DATA_TYPES;
	private static final Map<StoredDataType, PropertyType> DATA_TYPES_REV;

	private static final Map<StoredObjectType, BidiMap<String, IntermediateAttribute>> ATTRIBUTE_MAPPINGS;
	private static final Map<StoredObjectType, BidiMap<String, IntermediateProperty>> PROPERTY_MAPPINGS;

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
		Map<StoredObjectType, BidiMap<String, IntermediateProperty>> propertyMappings = new EnumMap<StoredObjectType, BidiMap<String, IntermediateProperty>>(
			StoredObjectType.class);

		BidiMap<String, IntermediateAttribute> am = null;
		BidiMap<String, IntermediateProperty> pm = null;

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		// BASE_TYPE_ID (ACL)
		// OBJECT_TYPE_ID (DM_ACL)
		am.put(CmisCustomAttributes.ACL_OWNER.name, IntermediateAttribute.OWNER);
		attributeMappings.put(StoredObjectType.ACL, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.ACL, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		// BASE_TYPE_ID (DOCUMENT)
		// OBJECT_TYPE_ID (cmis:document|...)
		am.put(CmisCustomAttributes.VERSION_ANTECEDENT_ID.name, IntermediateAttribute.VERSION_ANTECEDENT_ID);
		attributeMappings.put(StoredObjectType.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		pm.put(CmisCustomProperties.TARGET_PATHS.name, IntermediateProperty.PATH);
		pm.put(CmisCustomProperties.TARGET_PARENTS.name, IntermediateProperty.PARENT_ID);
		pm.put(CmisCustomProperties.CONTENTS.name, IntermediateProperty.CONTENT_STREAM_ID);
		pm.put(CmisCustomProperties.CURRENT_VERSION.name, IntermediateProperty.CURRENT_VERSION);
		propertyMappings.put(StoredObjectType.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		ATTRIBUTE_MAPPINGS = Tools.freezeMap(attributeMappings);
		PROPERTY_MAPPINGS = Tools.freezeMap(propertyMappings);
	}

	public static StoredDataType decodePropertyType(PropertyType t) {
		return CmisTranslator.DATA_TYPES.get(t);
	}

	public static PropertyType decodePropertyType(StoredObjectType t) {
		return CmisTranslator.DATA_TYPES_REV.get(t);
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
		return CmisTranslator.ATTRIBUTE_MAPPINGS.get(type);
	}

	private BidiMap<String, IntermediateProperty> getPropertyMappings(StoredObjectType type) {
		return CmisTranslator.PROPERTY_MAPPINGS.get(type);
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
	public String encodePropertyName(StoredObjectType type, String propertyName) {
		BidiMap<String, IntermediateProperty> mappings = getPropertyMappings(type);
		if (mappings != null) {
			// TODO: normalize the CMS property name
			IntermediateProperty prop = mappings.get(propertyName);
			if (prop != null) { return prop.encode(); }
		}
		return super.encodePropertyName(type, propertyName);
	}

	@Override
	public String decodePropertyName(StoredObjectType type, String propertyName) {
		BidiMap<String, IntermediateProperty> mappings = getPropertyMappings(type);
		if (mappings != null) {
			String prop = null;
			try {
				// TODO: normalize the intermediate property name
				prop = mappings.getKey(IntermediateProperty.decode(propertyName));
			} catch (IllegalArgumentException e) {
				prop = null;
			}
			if (prop != null) { return prop; }
		}
		return super.decodePropertyName(type, propertyName);
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