package com.armedia.cmf.storage;

import java.util.Date;

import com.armedia.commons.utilities.Tools;

abstract class CmsValueBase<T extends Object> implements CmsValue<T> {

	private final CmsDataType type;
	private final T value;

	protected CmsValueBase(CmsDataType type, T value) {
		if (type == null) { throw new IllegalArgumentException("Must provide a valid data type"); }
		this.type = type;
		this.value = value;
	}

	@Override
	public final T getValue() {
		return this.value;
	}

	@Override
	public final CmsDataType getDataType() {
		return this.type;
	}

	@Override
	public final Boolean asBoolean() {
		return convertValue(Boolean.class, CmsDataType.BOOLEAN);
	}

	@Override
	public final Integer asInteger() {
		return convertValue(Integer.class, CmsDataType.INTEGER);
	}

	@Override
	public final Double asDouble() {
		return convertValue(Double.class, CmsDataType.DOUBLE);
	}

	@Override
	public final String asString() {
		return convertValue(String.class, CmsDataType.STRING);
	}

	@Override
	public final Date asTemporal() {
		return convertValue(Date.class, CmsDataType.TEMPORAL);
	}

	@Override
	public final boolean isNull() {
		return (this.value == null);
	}

	@Override
	public final String getEncoded() {
		return encode(this.value);
	}

	@Override
	public final CmsValue<?> convert(CmsDataType type) {
		if (supports(type)) {
			switch (type) {
				case BOOLEAN:
					return new CmsValueBoolean(convertValue(Boolean.class, type));
				case INTEGER:
					return new CmsValueInteger(convertValue(Integer.class, type));
				case DOUBLE:
					return new CmsValueDouble(convertValue(Double.class, type));
				case STRING:
					return new CmsValueString(convertValue(String.class, type));
				case TEMPORAL:
					return new CmsValueTemporal(convertValue(Date.class, type));
			}
		}
		throw new CmsValueConversionException(getDataType(), type, getValue());
	}

	final CmsValue<T> decodeNew(String value) {
		T v = decode(value);
		return constructNew(v);
	}

	protected abstract CmsValue<T> constructNew(T value);

	protected final <K extends Object> K convertValue(Class<K> k, CmsDataType type) {
		if (!supports(type)) { throw new CmsValueConversionException(getDataType(), type, getValue()); }
		if (type == getDataType()) { return k.cast(getValue()); }
		return k.cast(doConversion(type));
	}

	@Override
	public final boolean supports(CmsDataType type) {
		if (type == null) { throw new IllegalArgumentException("Must provide a data type to convert to"); }
		return ((type == getDataType()) || supportsConversionTo(type));
	}

	protected abstract boolean supportsConversionTo(CmsDataType type);

	protected abstract Object doConversion(CmsDataType type);

	@Override
	public final boolean equals(Object o) {
		if (!Tools.baseEquals(this, o)) { return false; }
		CmsValueBase<?> other = CmsValueBase.class.cast(o);
		if (this.type != other.type) { return false; }
		if (!Tools.equals(this.value, other.value)) { return false; }
		return true;
	}

	@Override
	public final int hashCode() {
		return Tools.hashTool(this, null, this.type, this.value);
	}

	protected abstract String encode(T value) throws CmsValueConversionException;

	protected abstract T decode(String value) throws CmsValueConversionException;
}