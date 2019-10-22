/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.store;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import com.armedia.commons.utilities.Tools;

public final class CmfValue {

	public static enum Type {
		//
		BOOLEAN("bool") {
			@Override
			protected Object doGetValue(CmfValue value) {
				return value.asBoolean();
			}
		},
		INTEGER("int") {
			@Override
			protected Object doGetValue(CmfValue value) {
				return value.asInteger();
			}
		},
		DOUBLE("dbl") {
			@Override
			protected Object doGetValue(CmfValue value) {
				return value.asDouble();
			}
		},
		STRING("str") {
			@Override
			protected Object doGetValue(CmfValue value) {
				return value.asString();
			}
		},
		ID {
			@Override
			protected Object doGetValue(CmfValue value) {
				return value.asId();
			}
		},
		DATETIME("date") {
			@Override
			protected Object doGetValue(CmfValue value) {
				try {
					return value.asTime();
				} catch (ParseException e) {
					throw new UnsupportedOperationException(
						String.format("Failed to convert value [%s] of type [%s] into a DATETIME value",
							value.asString(), value.getDataType()),
						e);
				}
			}
		},
		URI {
			@Override
			protected Object doGetValue(CmfValue value) {
				try {
					return new URI(value.asString());
				} catch (URISyntaxException e) {
					throw new UnsupportedOperationException(
						String.format("Failed to convert value [%s] of type [%s] into a URI value", value.asString(),
							value.getDataType()),
						e);
				}
			}
		},
		HTML {
			@Override
			protected Object doGetValue(CmfValue value) {
				return value.asString();
			}
		},
		BASE64_BINARY("bin") {
			@Override
			protected Object doGetValue(CmfValue value) {
				return value.asBinary();
			}

			@Override
			protected boolean equals(Object a, Object b) {
				if (a == b) { return true; }
				if ((a == null) || (b == null)) { return false; }
				if (!(a instanceof byte[])) { return false; }
				if (!(b instanceof byte[])) { return false; }
				return Arrays.equals((byte[]) a, (byte[]) b);
			}
		},
		OTHER("oth") {
			@Override
			protected Object doGetValue(CmfValue value) {
				throw new UnsupportedOperationException("Values of type OTHER can't be converted to");
			}
		},
		//
		;

		public final String abbrev;

		private Type() {
			this(null);
		}

		private Type(String abbreviation) {
			this.abbrev = StringUtils.lowerCase(Tools.coalesce(abbreviation, name()));
		}

		private static final Map<CmfValue.Type, CmfValue> NULL;
		private static final Map<String, CmfValue.Type> ABBREV;

		static {
			Map<CmfValue.Type, CmfValue> nvl = new EnumMap<>(CmfValue.Type.class);
			Map<String, CmfValue.Type> abb = new TreeMap<>();
			for (CmfValue.Type t : CmfValue.Type.values()) {
				CmfValue.Type o = abb.put(t.abbrev, t);
				if (o != null) {
					throw new RuntimeException(
						String.format("ERROR: The CmfValue.Type values %s and %s share the same abbreviation [%s]",
							t.name(), o.name(), t.abbrev));
				}
				try {
					nvl.put(t, new CmfValue(t, null));
				} catch (ParseException e) {
					throw new RuntimeException(
						String.format("Failed to create a CMF value with a null value for type [%s]", t), e);
				}
			}
			NULL = Tools.freezeMap(nvl);
			ABBREV = Tools.freezeMap(new LinkedHashMap<>(abb));
		}

		public final CmfValue getNull() {
			return CmfValue.Type.NULL.get(this);
		}

		public final CmfValueSerializer getSerializer() {
			return CmfValueSerializer.get(this);
		}

		public final Object getValue(CmfValue value) {
			if (value.getDataType() == this) { return value.asObject(); }
			try {
				return doGetValue(value);
			} catch (Exception e) {
				throw new UnsupportedOperationException(
					String.format("Failed to convert value [%s] of type [%s] into a %s value", value.asObject(),
						value.getDataType(), name()),
					e);
			}
		}

		protected boolean equals(Object a, Object b) {
			return Objects.equals(a, b);
		}

		protected abstract Object doGetValue(CmfValue value) throws Exception;

		public static CmfValue.Type decode(String value) {
			if (value == null) { return null; }
			try {
				return CmfValue.Type.valueOf(StringUtils.upperCase(value));
			} catch (final IllegalArgumentException e) {
				// Maybe an abbreviation?
				CmfValue.Type t = CmfValue.Type.ABBREV.get(StringUtils.lowerCase(value));
				if (t != null) { return t; }
				throw e;
			}
		}
	}

	private static final List<FastDateFormat> DATE_FORMATS;

	static {
		List<FastDateFormat> dateFormats = new LinkedList<>();
		dateFormats.add(DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT);
		dateFormats.add(DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT);
		dateFormats.add(DateFormatUtils.ISO_8601_EXTENDED_TIME_TIME_ZONE_FORMAT);
		dateFormats.add(DateFormatUtils.ISO_8601_EXTENDED_TIME_FORMAT);
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

	private final CmfValue.Type type;
	private final Object value;
	private final boolean nullValue;

	public CmfValue(long value) {
		this.type = CmfValue.Type.INTEGER;
		this.value = value;
		this.nullValue = false;
	}

	public CmfValue(boolean value) {
		this.type = CmfValue.Type.BOOLEAN;
		this.value = value;
		this.nullValue = false;
	}

	public CmfValue(double value) {
		this.type = CmfValue.Type.DOUBLE;
		this.value = value;
		this.nullValue = false;
	}

	public CmfValue(String value) {
		this.type = CmfValue.Type.STRING;
		this.value = value;
		this.nullValue = (value == null);
	}

	public CmfValue(URI value) {
		this.type = CmfValue.Type.URI;
		this.value = value;
		this.nullValue = (value == null);
	}

	public CmfValue(byte[] data) {
		this.type = CmfValue.Type.BASE64_BINARY;
		this.value = Base64.encodeBase64(data);
		this.nullValue = (this.value == null);
	}

	public CmfValue(Date value) {
		this.type = CmfValue.Type.DATETIME;
		this.value = value;
		this.nullValue = (value == null);
	}

	public CmfValue(Calendar value) {
		this.type = CmfValue.Type.DATETIME;
		this.value = (value != null ? value.getTime() : null);
		this.nullValue = (value == null);
	}

	public CmfValue(CmfValue.Type type, Object value) throws ParseException {
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

	public CmfValue.Type getDataType() {
		return this.type;
	}

	public boolean isNull() {
		return this.nullValue;
	}

	public boolean isNotNull() {
		return !isNull();
	}

	public String serialize() throws ParseException {
		return this.type.getSerializer().serialize(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfValue other = CmfValue.class.cast(obj);
		if (this.type != other.type) { return false; }
		if (!this.type.equals(this.value, other.value)) { return false; }
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

	public static CmfValue newValue(CmfValue.Type type, Object value) {
		try {
			return new CmfValue(type, value);
		} catch (ParseException e) {
			throw new RuntimeException(String.format("Can't convert [%s] as a %s", value, type), e);
		}
	}
}