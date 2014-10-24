package com.armedia.cmf.storage;

import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

public final class CmsValueFactory {
	private static final Map<CmsDataType, CmsValueBase<?>> NULL;

	static {
		Map<CmsDataType, CmsValueBase<?>> m = new EnumMap<CmsDataType, CmsValueBase<?>>(CmsDataType.class);
		m.put(CmsDataType.BOOLEAN, new CmsValueBoolean(null));
		m.put(CmsDataType.INTEGER, new CmsValueInteger((Integer) null));
		m.put(CmsDataType.DOUBLE, new CmsValueDouble((Double) null));
		m.put(CmsDataType.STRING, new CmsValueString(null));
		m.put(CmsDataType.DATETIME, new CmsValueDatetime(null));
		NULL = Collections.unmodifiableMap(m);
	}

	public static CmsValue<?> getNullValue(CmsDataType type) {
		return CmsValueFactory.getNullValueBase(type);
	}

	private static CmsValueBase<?> getNullValueBase(CmsDataType type) {
		if (type == null) { throw new IllegalArgumentException("Must provide a data type to decode for"); }
		return CmsValueFactory.NULL.get(type);
	}

	private CmsValueFactory() {
	}

	public static CmsValue<?> decode(CmsDataType type, String value) {
		return CmsValueFactory.getNullValueBase(type).decodeNew(value);
	}

	public static CmsValue<?> newValue(CmsDataType type, Object v) {
		if (type == null) { throw new IllegalArgumentException("Must provide a value type"); }
		switch (type) {
			case BOOLEAN:
				return new CmsValueBoolean(Boolean.class.cast(v));
			case INTEGER:
				return new CmsValueInteger(Integer.class.cast(v));
			case DOUBLE:
				return new CmsValueDouble(Double.class.cast(v));
			case STRING:
				return new CmsValueString(String.class.cast(v));
			case DATETIME:
				return new CmsValueDatetime(Date.class.cast(v));
			default:
				throw new IllegalArgumentException(String.format("Unsupported type [%s]", type));
		}
	}

	public static CmsValue<Boolean> newBooleanValue(boolean v) {
		return new CmsValueBoolean(v);
	}

	public static CmsValue<Integer> newIntValue(int v) {
		return new CmsValueInteger(v);
	}

	public static CmsValue<Integer> newIntValue(long v) {
		return new CmsValueInteger((int) v);
	}

	public static CmsValue<String> newStringValue(String v) {
		return new CmsValueString(v);
	}

	public static CmsValue<Date> newTimeValue(long v) {
		return CmsValueFactory.newTimeValue(new Date(v));
	}

	public static CmsValue<Date> newTimeValue(Date v) {
		return new CmsValueDatetime(v);
	}

	public static CmsValue<Double> newDoubleValue(float v) {
		return new CmsValueDouble(v);
	}

	public static CmsValue<Double> newDoubleValue(double v) {
		return new CmsValueDouble(v);
	}
}