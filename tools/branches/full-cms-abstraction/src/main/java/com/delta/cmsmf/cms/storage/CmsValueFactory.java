package com.delta.cmsmf.cms.storage;

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
		m.put(CmsDataType.TEMPORAL, new CmsValueTemporal(null));
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
		return new CmsValueTemporal(v);
	}

	public static CmsValue<Double> newDoubleValue(float v) {
		return new CmsValueDouble(v);
	}

	public static CmsValue<Double> newDoubleValue(double v) {
		return new CmsValueDouble(v);
	}
}