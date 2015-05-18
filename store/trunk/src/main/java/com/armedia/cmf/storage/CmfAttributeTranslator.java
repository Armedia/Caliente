package com.armedia.cmf.storage;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

import com.armedia.commons.utilities.Tools;

public abstract class CmfAttributeTranslator<V> {
	private static class Codec implements CmfValueCodec<CmfValue> {

		private final CmfDataType type;
		private final CmfValue nullValue;

		private Codec(CmfDataType type) {
			this.type = type;
			try {
				this.nullValue = new CmfValue(this.type, null);
			} catch (ParseException e) {
				throw new RuntimeException("Unexpected parse exception", e);
			}
		}

		@Override
		public CmfValue encodeValue(CmfValue value) throws CmfValueEncoderException {
			return Tools.coalesce(value, this.nullValue);
		}

		@Override
		public CmfValue decodeValue(CmfValue value) throws CmfValueDecoderException {
			return Tools.coalesce(value, this.nullValue);
		}

		@Override
		public boolean isNull(CmfValue value) {
			return value.isNull();
		}

		@Override
		public CmfValue getNull() {
			return this.nullValue;
		}
	};

	private static final Map<CmfDataType, Codec> CODECS;

	static {
		Map<CmfDataType, Codec> codecs = new EnumMap<CmfDataType, Codec>(CmfDataType.class);
		for (CmfDataType t : CmfDataType.values()) {
			codecs.put(t, new Codec(t));
		}
		CODECS = Tools.freezeMap(codecs);
	}

	public static CmfValueCodec<CmfValue> getStoredValueCodec(CmfDataType type) {
		return CmfAttributeTranslator.CODECS.get(type);
	}

	public final String encodeValue(CmfDataType value) {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to encode"); }
		return value.name();
	}

	public final CmfDataType decodeValue(String value) {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to decode"); }
		return CmfDataType.decodeString(value);
	}

	public abstract CmfValueCodec<V> getCodec(CmfDataType type);

	public CmfObject<V> decodeObject(CmfObject<V> rawObject) {
		return rawObject;
	}

	public CmfObject<V> encodeObject(CmfObject<V> rawObject) {
		return rawObject;
	}

	public String encodeAttributeName(CmfType type, String attributeName) {
		return attributeName;
	}

	public String decodeAttributeName(CmfType type, String attributeName) {
		return attributeName;
	}

	public abstract V getValue(CmfDataType type, Object value) throws ParseException;
}