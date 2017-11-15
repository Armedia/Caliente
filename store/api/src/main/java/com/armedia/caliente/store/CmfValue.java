package com.armedia.caliente.store;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import com.armedia.commons.utilities.Tools;

public final class CmfValue {

	private static final List<FastDateFormat> DATE_FORMATS;

	static {
		List<FastDateFormat> dateFormats = new LinkedList<>();
		dateFormats.add(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT);
		dateFormats.add(DateFormatUtils.ISO_DATETIME_FORMAT);
		dateFormats.add(DateFormatUtils.ISO_DATE_TIME_ZONE_FORMAT);
		dateFormats.add(DateFormatUtils.ISO_DATE_FORMAT);
		dateFormats.add(DateFormatUtils.ISO_TIME_TIME_ZONE_FORMAT);
		dateFormats.add(DateFormatUtils.ISO_TIME_FORMAT);
		dateFormats.add(DateFormatUtils.ISO_TIME_NO_T_TIME_ZONE_FORMAT);
		dateFormats.add(DateFormatUtils.ISO_TIME_NO_T_FORMAT);
		dateFormats.add(DateFormatUtils.SMTP_DATETIME_FORMAT);
		DATE_FORMATS = Tools.freezeList(dateFormats);
	}

	public static Date parseDate(String str) throws ParseException {
		if (str == null) { return null; }
		for (FastDateFormat fmt : CmfValue.DATE_FORMATS) {
			try {
				return fmt.parse(str);
			} catch (ParseException e) {
				// Not in this format...
				continue;
			}
		}
		return DateFormat.getDateInstance().parse(str);
	}

	private final CmfDataType type;
	private final Object value;
	private final boolean nullValue;

	public CmfValue(long value) {
		this.type = CmfDataType.INTEGER;
		this.value = value;
		this.nullValue = false;
	}

	public CmfValue(boolean value) {
		this.type = CmfDataType.BOOLEAN;
		this.value = value;
		this.nullValue = false;
	}

	public CmfValue(double value) {
		this.type = CmfDataType.DOUBLE;
		this.value = value;
		this.nullValue = false;
	}

	public CmfValue(String value) {
		this.type = CmfDataType.STRING;
		this.value = value;
		this.nullValue = (value == null);
	}

	public CmfValue(URI value) {
		this.type = CmfDataType.URI;
		this.value = value;
		this.nullValue = (value == null);
	}

	public CmfValue(byte[] data) {
		this.type = CmfDataType.BASE64_BINARY;
		this.value = Base64.encodeBase64(data);
		this.nullValue = (this.value == null);
	}

	public CmfValue(Date value) {
		this.type = CmfDataType.DATETIME;
		this.value = value;
		this.nullValue = (value == null);
	}

	public CmfValue(Calendar value) {
		this.type = CmfDataType.DATETIME;
		this.value = (value != null ? value.getTime() : null);
		this.nullValue = (value == null);
	}

	public CmfValue(CmfDataType type, Object value) throws ParseException {
		this.type = type;
		this.nullValue = (value == null);
		if (value != null) {
			switch (type) {
				case INTEGER:
					if (value instanceof Number) {
						this.value = value;
					} else {
						this.value = Integer.valueOf(value.toString());
					}
					break;
				case BOOLEAN:
					if (value instanceof Boolean) {
						this.value = value;
					} else {
						this.value = Boolean.valueOf(value.toString());
					}
					break;
				case DOUBLE:
					if (value instanceof Number) {
						this.value = value;
					} else {
						this.value = Double.valueOf(value.toString());
					}
					break;
				case BASE64_BINARY:
					if (value instanceof byte[]) {
						this.value = ((byte[]) value).clone();
					} else {
						this.value = Base64.decodeBase64(value.toString());
					}
					break;
				case URI:
					if (URI.class.isInstance(value)) {
						this.value = value;
					} else {
						try {
							this.value = new URI(value.toString());
						} catch (URISyntaxException e) {
							ParseException pe = new ParseException(value.toString(), 0);
							pe.initCause(e);
							throw pe;
						}
					}
					break;
				case HTML:
				case STRING:
				case ID:
					this.value = Tools.toString(value);
					break;
				case DATETIME:
					if (value instanceof Date) {
						this.value = Date.class.cast(value);
					} else if (value instanceof Calendar) {
						this.value = Calendar.class.cast(value).getTime();
					} else {
						this.value = CmfValue.parseDate(value.toString());
					}
					break;
				default:
					throw new IllegalArgumentException(String.format("Unsupported data type [%s]", type));
			}
		} else {
			this.value = value;
		}
	}

	public String asString() {
		return Tools.toString(this.value);
	}

	public String asId() {
		return asString();
	}

	public int asInteger() {
		if (this.nullValue) { return 0; }
		if (this.value instanceof Number) { return Number.class.cast(this.value).intValue(); }
		return Integer.valueOf(this.value.toString());
	}

	public long asLong() {
		if (this.nullValue) { return 0; }
		if (this.value instanceof Number) { return Number.class.cast(this.value).longValue(); }
		return Long.valueOf(this.value.toString());
	}

	public boolean asBoolean() {
		if (this.nullValue) { return false; }
		if (this.value instanceof Boolean) { return Boolean.class.cast(this.value).booleanValue(); }
		return Boolean.valueOf(this.value.toString());
	}

	public double asDouble() {
		if (this.nullValue) { return Double.NaN; }
		if (this.value instanceof Number) { return Number.class.cast(this.value).doubleValue(); }
		return Double.valueOf(this.value.toString());
	}

	public Date asTime() throws ParseException {
		if (this.nullValue) { return null; }
		if (this.value instanceof Date) { return Date.class.cast(this.value); }
		return CmfValue.parseDate(this.value.toString());
	}

	public byte[] asBinary() {
		if (this.nullValue) { return null; }
		if (this.value instanceof byte[]) { return ((byte[]) this.value).clone(); }
		return Base64.decodeBase64(this.value.toString());
	}

	public URI asURI() throws URISyntaxException {
		if (this.nullValue) { return null; }
		if (this.value instanceof URI) { return URI.class.cast(this.value); }
		return new URI(this.value.toString());
	}

	public Object asObject() {
		return this.value;
	}

	public CmfDataType getDataType() {
		return this.type;
	}

	public boolean isNull() {
		return this.nullValue;
	}

	public String serialize() throws ParseException {
		return this.type.getSerializer().serialize(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfValue other = CmfValue.class.cast(obj);
		if (this.type != other.type) { return false; }
		if (!Tools.equals(this.value, other.value)) { return false; }
		return true;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.type, this.value);
	}

	@Override
	public String toString() {
		return asString();
	}

	public static CmfValue newValue(CmfDataType type, Object value) {
		try {
			return new CmfValue(type, value);
		} catch (ParseException e) {
			throw new RuntimeException(String.format("Can't convert [%s] as a %s", value, type), e);
		}
	}
}