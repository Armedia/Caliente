package com.delta.cmsmf.cms.storage;

import com.armedia.commons.utilities.Tools;

public final class CmsValueInteger extends CmsValueBase<Integer> {

	public CmsValueInteger(Byte value) {
		super(CmsDataType.INTEGER, (value != null ? value.intValue() : null));
	}

	public CmsValueInteger(Short value) {
		super(CmsDataType.INTEGER, (value != null ? value.intValue() : null));
	}

	public CmsValueInteger(Integer value) {
		super(CmsDataType.INTEGER, value);
	}

	public CmsValueInteger(Long value) {
		super(CmsDataType.INTEGER, (value != null ? value.intValue() : null));
	}

	@Override
	protected CmsValueInteger constructNew(Integer v) {
		return new CmsValueInteger(v);
	}

	@Override
	protected String encode(Integer value) {
		return Tools.toString(value);
	}

	@Override
	protected Integer decode(String value) {
		return Tools.decodeInteger(value);
	}

	@Override
	public boolean supportsConversionTo(CmsDataType targetType) {
		return (targetType != CmsDataType.TEMPORAL);
	}

	@Override
	protected Object doConversion(CmsDataType type) {
		Integer v = getValue();
		if (v == null) { return null; }
		switch (type) {
			case BOOLEAN:
				return (v.intValue() != 0);

			case INTEGER:
				return v;

			case DOUBLE:
				return v.doubleValue();

			case STRING:
				return v.toString();

			default:
				// Should never happen, but choke anyway
				throw new CmsValueConversionException(getDataType(), type, getValue());
		}
	}
}