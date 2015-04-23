package com.armedia.cmf.storage;

import java.text.ParseException;

public abstract class ObjectStorageTranslator<V> {

	public final String encodeValue(StoredDataType value) {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to encode"); }
		return value.name();
	}

	public final StoredDataType decodeValue(String value) {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to decode"); }
		return StoredDataType.decodeString(value);
	}

	public abstract StoredValueCodec<V> getCodec(StoredDataType type);

	public StoredObject<V> decodeObject(StoredObject<V> rawObject) {
		return rawObject;
	}

	public StoredObject<V> encodeObject(StoredObject<V> rawObject) {
		return rawObject;
	}

	public String encodeAttributeName(StoredObjectType type, String attributeName) {
		return attributeName;
	}

	public String decodeAttributeName(StoredObjectType type, String attributeName) {
		return attributeName;
	}

	public String encodePropertyName(StoredObjectType type, String propertyName) {
		return propertyName;
	}

	public String decodePropertyName(StoredObjectType type, String propertyName) {
		return propertyName;
	}

	public abstract V getValue(StoredDataType type, Object value) throws ParseException;
}