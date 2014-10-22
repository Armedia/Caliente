package com.armedia.cmf.storage;

import com.armedia.commons.utilities.Tools;

public final class CmsValueBoolean extends CmsValueBase<Boolean> {

	public CmsValueBoolean(Boolean value) {
		super(CmsDataType.BOOLEAN, value);
	}

	@Override
	protected CmsValueBoolean constructNew(Boolean v) {
		return new CmsValueBoolean(v);
	}

	@Override
	protected String encode(Boolean value) {
		return Tools.toString(value);
	}

	@Override
	protected Boolean decode(String value) {
		return Tools.decodeBoolean(value);
	}

	@Override
	protected boolean supportsConversionTo(CmsDataType targetType) {
		return (targetType != CmsDataType.TEMPORAL);
	}

	@Override
	protected Object doConversion(CmsDataType type) {
		Boolean v = getValue();
		if (v == null) { return null; }
		switch (type) {
			case BOOLEAN:
				return v;

			case INTEGER:
			case DOUBLE:
				return (v ? 1 : 0);

			case STRING:
				return v.toString();

			default:
				// Should never happen, but choke anyway
				throw new CmsValueConversionException(getDataType(), type, getValue());
		}
	}
}