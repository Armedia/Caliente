package com.armedia.cmf.storage;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

public final class CmsValueTemporal extends CmsValueBase<Date> {

	static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss.SSS";

	public CmsValueTemporal(Date value) {
		super(CmsDataType.TEMPORAL, value);
	}

	@Override
	protected CmsValueTemporal constructNew(Date v) {
		return new CmsValueTemporal(v);
	}

	@Override
	protected String encode(Date value) {
		if (isNull()) { return null; }
		return DateFormatUtils.format(getValue(), CmsValueTemporal.DATE_FORMAT);
	}

	@Override
	protected Date decode(String value) {
		return CmsValueTemporal.doDecode(value);
	}

	@Override
	public boolean supportsConversionTo(CmsDataType targetType) {
		switch (targetType) {
			case TEMPORAL:
			case STRING:
				return true;
			default:
				return false;
		}
	}

	@Override
	protected Object doConversion(CmsDataType type) {
		Date v = getValue();
		if (v == null) { return null; }
		switch (type) {
			case STRING:
				return encode(v);

			case TEMPORAL:
				return v;

			default:
				// Should never happen, but choke anyway
				throw new CmsValueConversionException(getDataType(), type, getValue());
		}
	}

	static Date doDecode(String value) {
		if (value == null) { return null; }
		try {
			return DateUtils.parseDate(value, CmsValueTemporal.DATE_FORMAT);
		} catch (ParseException e) {
			throw new RuntimeException(String.format("Failed to convert the string [%s] to a temporal value", value), e);
		}
	}
}