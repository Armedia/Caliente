package com.armedia.cmf.storage;

public abstract class ObjectStorageTranslator<V> implements StoredValueCodec<StoredDataType> {

	@Override
	public final String encodeValue(StoredDataType value) {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to encode"); }
		return value.name();
	}

	@Override
	public final StoredDataType decodeValue(String value) {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to decode"); }
		return StoredDataType.valueOf(value);
	}

	public abstract StoredValueCodec<V> getCodec(StoredDataType type);

}