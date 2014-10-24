package com.armedia.cmf.storage;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

public final class CmsValueDatetime extends CmsValueBase<Date> {

	static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss.SSS";

	public CmsValueDatetime(Date value) {
		super(CmsDataType.DATETIME, value);
	}

	@Override
	protected CmsValueDatetime constructNew(Date v) {
		return new CmsValueDatetime(v);
	}

	@Override
	protected String encode(Date value) {
		if (isNull()) { return null; }
		return DateFormatUtils.format(getValue(), CmsValueDatetime.DATE_FORMAT);
	}

	@Override
	protected Date decode(String value) {
		return CmsValueDatetime.doDecode(value);
	}

	@Override
	public boolean supportsConversionTo(CmsDataType targetType) {
		switch (targetType) {
			case DATETIME:
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

			case DATETIME:
				return v;

			default:
				// Should never happen, but choke anyway
				throw new CmsValueConversionException(getDataType(), type, getValue());
		}
	}

	static Date doDecode(String value) {
		if (value == null) { return null; }
		try {
			return DateUtils.parseDate(value, CmsValueDatetime.DATE_FORMAT);
		} catch (ParseException e) {
			throw new RuntimeException(String.format("Failed to convert the string [%s] to a temporal value", value), e);
		}
	}
}