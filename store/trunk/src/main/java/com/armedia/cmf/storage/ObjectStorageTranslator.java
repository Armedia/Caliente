package com.armedia.cmf.storage;

public abstract class ObjectStorageTranslator<T, V> implements StoredValueCodec<StoredDataType> {

	public final StoredObjectType decodeObjectType(T object) throws UnsupportedObjectTypeException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose type to decode"); }
		return doDecodeObjectType(object);
	}

	protected abstract StoredObjectType doDecodeObjectType(T object) throws UnsupportedObjectTypeException;

	public final Class<? extends T> decodeObjectType(StoredObjectType type) throws UnsupportedObjectTypeException {
		if (type == null) { throw new IllegalArgumentException("Must provide a type whose object class to decode"); }
		return doDecodeObjectType(type);
	}

	protected abstract Class<? extends T> doDecodeObjectType(StoredObjectType type)
		throws UnsupportedObjectTypeException;

	public final String getObjectId(T object) throws Exception {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose ID to retrieve"); }
		return doGetObjectId(object);
	}

	protected abstract String doGetObjectId(T object) throws Exception;

	@Override
	public final String encodeValue(StoredDataType value) {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to encode"); }
		return value.name();
	}

	@Override
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

	public String encodePropertyName(StoredObjectType type, String attributeName) {
		return attributeName;
	}

	public String decodePropertyName(StoredObjectType type, String attributeName) {
		return attributeName;
	}
}