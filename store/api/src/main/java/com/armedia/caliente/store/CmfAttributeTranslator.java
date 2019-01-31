package com.armedia.caliente.store;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import com.armedia.commons.utilities.Tools;

public abstract class CmfAttributeTranslator<VALUE> {
	private static class Codec implements CmfValueCodec<CmfValue> {

		private final CmfValue.Type type;
		private final CmfValue nullValue;

		private Codec(CmfValue.Type type) {
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
			return (value == null) || value.isNull();
		}

		@Override
		public CmfValue getNull() {
			return this.nullValue;
		}
	};

	private static class DefaultValueCodec implements CmfValueCodec<CmfValue> {

		private final CmfValue.Type type;

		private DefaultValueCodec(CmfValue.Type type) {
			this.type = type;
		}

		@Override
		public boolean isNull(CmfValue value) {
			return (value == null) || value.isNull();
		}

		@Override
		public CmfValue getNull() {
			return this.type.getNull();
		}

		@Override
		public CmfValue encodeValue(CmfValue value) {
			return value;
		}

		@Override
		public CmfValue decodeValue(CmfValue value) {
			return value;
		}
	};

	public static final CmfAttributeNameMapper NULL_MAPPER = new CmfAttributeNameMapper();

	private static final Map<CmfValue.Type, Codec> CODECS;

	static {
		Map<CmfValue.Type, Codec> codecs = new EnumMap<>(CmfValue.Type.class);
		for (CmfValue.Type t : CmfValue.Type.values()) {
			codecs.put(t, new Codec(t));
		}
		CODECS = Tools.freezeMap(codecs);
	}

	public static final CmfAttributeTranslator<CmfValue> CMFVALUE_TRANSLATOR = new CmfAttributeTranslator<CmfValue>(
		CmfValue.class) {

		private final Map<CmfValue.Type, CmfValueCodec<CmfValue>> codecs;

		{
			Map<CmfValue.Type, CmfValueCodec<CmfValue>> codecs = new EnumMap<>(CmfValue.Type.class);
			for (CmfValue.Type t : CmfValue.Type.values()) {
				codecs.put(t, new DefaultValueCodec(t));
			}
			this.codecs = Tools.freezeMap(codecs);
		}

		@Override
		public CmfValueCodec<CmfValue> getCodec(CmfValue.Type type) {
			return this.codecs.get(type);
		}

		@Override
		public CmfValue getValue(CmfValue.Type type, Object value) throws ParseException {
			return new CmfValue(type, value);
		}

	};

	public static CmfValueCodec<CmfValue> getStoredValueCodec(CmfValue.Type type) {
		return CmfAttributeTranslator.CODECS.get(type);
	}

	private final Class<VALUE> valueClass;
	private final CmfAttributeNameMapper nameMapper;

	protected CmfAttributeTranslator(Class<VALUE> valueClass) {
		this(valueClass, null);
	}

	protected CmfAttributeTranslator(Class<VALUE> valueClass, CmfAttributeNameMapper cmfAttributeNameMapper) {
		this.valueClass = Objects.requireNonNull(valueClass, "Must provide a value class");
		this.nameMapper = Tools.coalesce(cmfAttributeNameMapper, CmfAttributeTranslator.NULL_MAPPER);
	}

	public final CmfAttributeNameMapper getAttributeNameMapper() {
		return this.nameMapper;
	}

	public abstract CmfValueCodec<VALUE> getCodec(CmfValue.Type type);

	public abstract VALUE getValue(CmfValue.Type type, Object value) throws ParseException;

	public final CmfObject<VALUE> decodeObject(CmfObject<CmfValue> obj) {
		// Can we optimize this if there are no changes needed?
		if (this.valueClass.equals(CmfValue.class) && (this.nameMapper == CmfAttributeTranslator.NULL_MAPPER)) {
			@SuppressWarnings("unchecked")
			CmfObject<VALUE> ret = (CmfObject<VALUE>) obj;
			return ret;
		}

		CmfObject<VALUE> newObj = new CmfObject<>(//
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
			obj.getNumber() //
		);

		for (CmfAttribute<CmfValue> att : obj.getAttributes()) {
			String attName = this.nameMapper.decodeAttributeName(newObj.getType(), att.getName());
			CmfAttribute<VALUE> newAtt = new CmfAttribute<>(attName, att.getType(), att.isMultivalued());
			CmfValueCodec<VALUE> codec = getCodec(att.getType());
			if (newAtt.isMultivalued()) {
				for (CmfValue v : att) {
					newAtt.addValue(codec.decodeValue(v));
				}
			} else {
				newAtt.setValue(codec.decodeValue(att.getValue()));
			}
			newObj.setAttribute(newAtt);
		}

		for (CmfProperty<CmfValue> prop : obj.getProperties()) {
			CmfProperty<VALUE> newProp = new CmfProperty<>(prop.getName(), prop.getType(), prop.isMultivalued());
			CmfValueCodec<VALUE> codec = getCodec(prop.getType());
			if (newProp.isMultivalued()) {
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

	public final CmfObject<CmfValue> encodeObject(CmfObject<VALUE> obj) {
		// Can we optimize this if there are no changes needed?
		if (this.valueClass.equals(CmfValue.class) && (this.nameMapper == CmfAttributeTranslator.NULL_MAPPER)) {
			@SuppressWarnings("unchecked")
			CmfObject<CmfValue> ret = (CmfObject<CmfValue>) obj;
			return ret;
		}

		CmfObject<CmfValue> newObj = new CmfObject<>(//
			CmfAttributeTranslator.CMFVALUE_TRANSLATOR, //
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
			obj.getNumber() //
		);

		for (CmfAttribute<VALUE> att : obj.getAttributes()) {
			String attName = this.nameMapper.encodeAttributeName(newObj.getType(), att.getName());
			CmfAttribute<CmfValue> newAtt = new CmfAttribute<>(attName, att.getType(), att.isMultivalued());
			CmfValueCodec<VALUE> codec = getCodec(att.getType());
			if (newAtt.isMultivalued()) {
				for (VALUE v : att) {
					newAtt.addValue(codec.encodeValue(v));
				}
			} else {
				newAtt.setValue(codec.encodeValue(att.getValue()));
			}
			newObj.setAttribute(newAtt);
		}

		for (CmfProperty<VALUE> prop : obj.getProperties()) {
			CmfProperty<CmfValue> newProp = new CmfProperty<>(prop.getName(), prop.getType(), prop.isMultivalued());
			CmfValueCodec<VALUE> codec = getCodec(prop.getType());
			if (newProp.isMultivalued()) {
				for (VALUE v : prop) {
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