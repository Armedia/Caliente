package com.armedia.cmf.storage;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

import com.armedia.commons.utilities.Tools;

public abstract class ObjectStorageTranslator<V> {
	private static class Codec implements StoredValueCodec<StoredValue> {

		private final StoredDataType type;
		private final StoredValue nullValue;

		private Codec(StoredDataType type) {
			this.type = type;
			try {
				this.nullValue = new StoredValue(this.type, null);
			} catch (ParseException e) {
				throw new RuntimeException("Unexpected parse exception", e);
			}
		}

		@Override
		public StoredValue encodeValue(StoredValue value) throws StoredValueEncoderException {
			return Tools.coalesce(value, this.nullValue);
		}

		@Override
		public StoredValue decodeValue(StoredValue value) throws StoredValueDecoderException {
			return Tools.coalesce(value, this.nullValue);
		}

		@Override
		public boolean isNull(StoredValue value) {
			return value.isNull();
		}

		@Override
		public StoredValue getNull() {
			return this.nullValue;
		}
	};

	private static final Map<StoredDataType, Codec> CODECS;

	static {
		Map<StoredDataType, Codec> codecs = new EnumMap<StoredDataType, Codec>(StoredDataType.class);
		for (StoredDataType t : StoredDataType.values()) {
			codecs.put(t, new Codec(t));
		}
		CODECS = Tools.freezeMap(codecs);
	}

	public static StoredValueCodec<StoredValue> getStoredValueCodec(StoredDataType type) {
		return ObjectStorageTranslator.CODECS.get(type);
	}

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