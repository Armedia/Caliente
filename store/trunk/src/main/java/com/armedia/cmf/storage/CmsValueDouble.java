package com.armedia.cmf.storage;

public final class CmsValueDouble extends CmsValueBase<Double> {

	public CmsValueDouble(Float value) {
		super(CmsDataType.DOUBLE, (value != null ? value.doubleValue() : null));
	}

	public CmsValueDouble(Double value) {
		super(CmsDataType.DOUBLE, value);
	}

	@Override
	protected CmsValueDouble constructNew(Double v) {
		return new CmsValueDouble(v);
	}

	@Override
	protected String encode(Double value) {
		if (value == null) { return null; }
		long bits = Double.doubleToRawLongBits(value.doubleValue());
		return String.valueOf(bits);
	}

	@Override
	protected Double decode(String value) {
		if (value == null) { return null; }
		long bits = Long.valueOf(value);
		return Double.longBitsToDouble(bits);
	}

	@Override
	public boolean supportsConversionTo(CmsDataType targetType) {
		return (targetType != CmsDataType.TEMPORAL);
	}

	@Override
	protected Object doConversion(CmsDataType type) {
		Double v = getValue();
		if (v == null) { return null; }
		switch (type) {
			case BOOLEAN:
				return (v.intValue() != 0);

			case INTEGER:
				return v.intValue();

			case DOUBLE:
				return v;

			case STRING:
				return v.toString();

			default:
				// Should never happen, but choke anyway
				throw new CmsValueConversionException(getDataType(), type, getValue());
		}
	}
}