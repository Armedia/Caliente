package com.armedia.caliente.store;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.armedia.commons.utilities.BaseCodec;
import com.armedia.commons.utilities.Tools;

public abstract class CmfAttributeTranslator<VALUE> {

	private static class DefaultValueCodec extends BaseCodec<CmfValue, CmfValue> implements CmfValueCodec<CmfValue> {
		private DefaultValueCodec(CmfValue.Type type) {
			super(Function.identity(), type.getNull(), CmfValue::isNull, Function.identity(), type.getNull(),
				CmfValue::isNull);
		}
	};

	public static final CmfAttributeNameMapper NULL_MAPPER = new CmfAttributeNameMapper();

	private static final Map<CmfValue.Type, CmfValueCodec<CmfValue>> CODECS;

	static {
		Map<CmfValue.Type, CmfValueCodec<CmfValue>> codecs = new EnumMap<>(CmfValue.Type.class);
		for (CmfValue.Type t : CmfValue.Type.values()) {
			codecs.put(t, new DefaultValueCodec(t));
		}
		CODECS = Tools.freezeMap(codecs);
	}

	public static final CmfAttributeTranslator<CmfValue> CMFVALUE_TRANSLATOR = new CmfAttributeTranslator<CmfValue>(
		CmfValue.class) {

		@Override
		public CmfValueCodec<CmfValue> getCodec(CmfValue.Type type) {
			return CmfAttributeTranslator.CODECS.get(type);
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
					newAtt.addValue(codec.decode(v));
				}
			} else {
				newAtt.setValue(codec.decode(att.getValue()));
			}
			newObj.setAttribute(newAtt);
		}

		for (CmfProperty<CmfValue> prop : obj.getProperties()) {
			CmfProperty<VALUE> newProp = new CmfProperty<>(prop.getName(), prop.getType(), prop.isMultivalued());
			CmfValueCodec<VALUE> codec = getCodec(prop.getType());
			if (newProp.isMultivalued()) {
				for (CmfValue v : prop) {
					newProp.addValue(codec.decode(v));
				}
			} else {
				newProp.setValue(codec.decode(prop.getValue()));
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
					newAtt.addValue(codec.encode(v));
				}
			} else {
				newAtt.setValue(codec.encode(att.getValue()));
			}
			newObj.setAttribute(newAtt);
		}

		for (CmfProperty<VALUE> prop : obj.getProperties()) {
			CmfProperty<CmfValue> newProp = new CmfProperty<>(prop.getName(), prop.getType(), prop.isMultivalued());
			CmfValueCodec<VALUE> codec = getCodec(prop.getType());
			if (newProp.isMultivalued()) {
				for (VALUE v : prop) {
					newProp.addValue(codec.encode(v));
				}
			} else {
				newProp.setValue(codec.encode(prop.getValue()));
			}
			newObj.setProperty(newProp);
		}

		return newObj;
	}
}