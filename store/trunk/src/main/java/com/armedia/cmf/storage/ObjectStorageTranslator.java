package com.armedia.cmf.storage;

import java.text.ParseException;

public abstract class ObjectStorageTranslator<V> {

	public final StoredObjectType decodeObjectType(Object object) throws UnsupportedObjectTypeException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose type to decode"); }
		return doDecodeObjectType(object);
	}

	protected abstract StoredObjectType doDecodeObjectType(Object object) throws UnsupportedObjectTypeException;

	public final Class<?> decodeObjectType(StoredObjectType type) throws UnsupportedObjectTypeException {
		if (type == null) { throw new IllegalArgumentException("Must provide a type whose object class to decode"); }
		return doDecodeObjectType(type);
	}

	protected abstract Class<?> doDecodeObjectType(StoredObjectType type) throws UnsupportedObjectTypeException;

	public final String getObjectId(Object object) throws Exception {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose ID to retrieve"); }
		return doGetObjectId(object);
	}

	protected abstract String doGetObjectId(Object object) throws Exception;

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