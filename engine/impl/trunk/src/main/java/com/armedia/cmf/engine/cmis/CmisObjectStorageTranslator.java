package com.armedia.cmf.engine.cmis;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.enums.PropertyType;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.cmf.storage.StoredValueCodec;
import com.armedia.commons.utilities.Tools;

public class CmisObjectStorageTranslator extends ObjectStorageTranslator<StoredValue> {

	private static final Map<PropertyType, StoredDataType> DATA_TYPES;
	private static final Map<StoredDataType, PropertyType> DATA_TYPES_REV;
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
	}

	public static StoredDataType decodePropertyType(PropertyType t) {
		return CmisObjectStorageTranslator.DATA_TYPES.get(t);
	}

	public static PropertyType decodePropertyType(StoredObjectType t) {
		return CmisObjectStorageTranslator.DATA_TYPES_REV.get(t);
	}

	@Override
	public StoredObject<StoredValue> decodeObject(StoredObject<StoredValue> rawObject) {
		return super.decodeObject(rawObject);
	}

	@Override
	public StoredObject<StoredValue> encodeObject(StoredObject<StoredValue> rawObject) {
		return super.encodeObject(rawObject);
	}

	@Override
	public String encodeAttributeName(StoredObjectType type, String attributeName) {
		return super.encodeAttributeName(type, attributeName);
	}

	@Override
	public String decodeAttributeName(StoredObjectType type, String attributeName) {
		return super.decodeAttributeName(type, attributeName);
	}

	@Override
	public String encodePropertyName(StoredObjectType type, String propertyName) {
		return super.encodePropertyName(type, propertyName);
	}

	@Override
	public String decodePropertyName(StoredObjectType type, String propertyName) {
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