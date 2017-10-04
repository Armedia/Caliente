package com.armedia.caliente.store;

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
		public CmfValue encodeValue(CmfValue value) {
			return Tools.coalesce(value, this.nullValue);
		}

		@Override
		public CmfValue decodeValue(CmfValue value) {
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

	private static final CmfAttributeNameMapper NULL_MAPPER = new CmfAttributeNameMapper();

	private static final Map<CmfDataType, Codec> CODECS;

	static {
		Map<CmfDataType, Codec> codecs = new EnumMap<>(CmfDataType.class);
		for (CmfDataType t : CmfDataType.values()) {
			codecs.put(t, new Codec(t));
		}
		CODECS = Tools.freezeMap(codecs);
	}

	public static CmfValueCodec<CmfValue> getStoredValueCodec(CmfDataType type) {
		return CmfAttributeTranslator.CODECS.get(type);
	}

	private final Class<V> valueClass;
	private final CmfAttributeNameMapper cmfAttributeNameMapper;

	protected CmfAttributeTranslator(Class<V> valueClass) {
		this(valueClass, null);
	}

	protected CmfAttributeTranslator(Class<V> valueClass, CmfAttributeNameMapper cmfAttributeNameMapper) {
		this.valueClass = valueClass;
		this.cmfAttributeNameMapper = Tools.coalesce(cmfAttributeNameMapper, CmfAttributeTranslator.NULL_MAPPER);
	}

	public final String encodeValue(CmfDataType value) {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to encode"); }
		return value.name();
	}

	public final CmfDataType decodeValue(String value) {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to decode"); }
		return CmfDataType.decodeString(value);
	}

	public final CmfAttributeNameMapper getAttributeNameMapper() {
		return this.cmfAttributeNameMapper;
	}

	public abstract CmfValueCodec<V> getCodec(CmfDataType type);

	public abstract V getValue(CmfDataType type, Object value) throws ParseException;

	public abstract String getDefaultSubtype(CmfType baseType);

	public final CmfObject<V> decodeObject(CmfObject<CmfValue> obj) {
		// Can we optimize this if there are no changes needed?
		if (this.valueClass.equals(CmfValue.class)
			&& (this.cmfAttributeNameMapper == CmfAttributeTranslator.NULL_MAPPER)) {
			@SuppressWarnings("unchecked")
			CmfObject<V> ret = (CmfObject<V>) obj;
			return ret;
		}

		CmfObject<V> newObj = new CmfObject<>(//
			this, //
			obj.getType(), //
			obj.getId(), //
			obj.getName(), //
			obj.getParentReferences(), //
			obj.getDependencyTier(), //
			obj.getHistoryId(), //
			obj.isHistoryCurrent(), //
			obj.getLabel(), //
			obj.getSubtype(), //
			obj.getSecondarySubtypes(), //
			obj.getProductName(), //
			obj.getProductVersion(), //
			obj.getNumber() //
		);

		for (CmfAttribute<CmfValue> att : obj.getAttributes()) {
			CmfAttribute<V> newAtt = new CmfAttribute<>(att.getName(), att.getType(), att.isRepeating());
			CmfValueCodec<V> codec = getCodec(att.getType());
			if (newAtt.isRepeating()) {
				for (CmfValue v : att) {
					newAtt.addValue(codec.decodeValue(v));
				}
			} else {
				newAtt.setValue(codec.decodeValue(att.getValue()));
			}
			newObj.setAttribute(newAtt);
		}

		for (CmfProperty<CmfValue> prop : obj.getProperties()) {
			CmfProperty<V> newProp = new CmfProperty<>(prop.getName(), prop.getType(), prop.isRepeating());
			CmfValueCodec<V> codec = getCodec(prop.getType());
			if (newProp.isRepeating()) {
				for (CmfValue v : prop) {
					newProp.addValue(codec.decodeValue(v));
				}
			} else {
				newProp.setValue(codec.decodeValue(prop.getValue()));
			}
			newObj.setProperty(newProp);
		}

		return newObj;
	}

	public final CmfObject<CmfValue> encodeObject(CmfObject<V> obj) {
		CmfObject<CmfValue> newObj = new CmfObject<>(//
			null, //
			obj.getType(), //
			obj.getId(), //
			obj.getName(), //
			obj.getParentReferences(), //
			obj.getDependencyTier(), //
			obj.getHistoryId(), //
			obj.isHistoryCurrent(), //
			obj.getLabel(), //
			obj.getSubtype(), //
			obj.getSecondarySubtypes(), //
			obj.getProductName(), //
			obj.getProductVersion(), //
			obj.getNumber() //
		);

		for (CmfAttribute<V> att : obj.getAttributes()) {
			CmfAttribute<CmfValue> newAtt = new CmfAttribute<>(att.getName(), att.getType(), att.isRepeating());
			CmfValueCodec<V> codec = getCodec(att.getType());
			if (newAtt.isRepeating()) {
				for (V v : att) {
					newAtt.addValue(codec.encodeValue(v));
				}
			} else {
				newAtt.setValue(codec.encodeValue(att.getValue()));
			}
			newObj.setAttribute(newAtt);
		}

		for (CmfProperty<V> prop : obj.getProperties()) {
			CmfProperty<CmfValue> newProp = new CmfProperty<>(prop.getName(), prop.getType(), prop.isRepeating());
			CmfValueCodec<V> codec = getCodec(prop.getType());
			if (newProp.isRepeating()) {
				for (V v : prop) {
					newProp.addValue(codec.encodeValue(v));
				}
			} else {
				newProp.setValue(codec.encodeValue(prop.getValue()));
			}
			newObj.setProperty(newProp);
		}

		return newObj;
	}
}