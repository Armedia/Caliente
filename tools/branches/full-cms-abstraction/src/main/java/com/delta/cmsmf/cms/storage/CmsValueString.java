package com.delta.cmsmf.cms.storage;


public final class CmsValueString extends CmsValueBase<String> {

	public CmsValueString(String value) {
		super(CmsDataType.STRING, value);
	}

	@Override
	protected CmsValueString constructNew(String v) {
		return new CmsValueString(v);
	}

	@Override
	public String encode(String value) {
		return value;
	}

	@Override
	protected String decode(String value) {
		return value;
	}

	@Override
	protected boolean supportsConversionTo(CmsDataType targetType) {
		return true;
	}

	@Override
	protected Object doConversion(CmsDataType type) {
		String v = getValue();
		if (v == null) { return null; }
		switch (type) {
			case BOOLEAN:
				return Boolean.valueOf(v);

			case INTEGER:
				return Integer.valueOf(v);

			case DOUBLE:
				return Double.valueOf(v);

			case STRING:
				return v;

			case TEMPORAL:
				return CmsValueTemporal.doDecode(v);

			default:
				// Should never happen, but choke anyway
				throw new CmsValueConversionException(getDataType(), type, getValue());
		}
	}
}