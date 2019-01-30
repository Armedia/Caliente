package com.armedia.caliente.store;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import com.armedia.commons.utilities.Tools;

public enum CmfValueSerializer {
	//
	BOOLEAN(CmfValueType.BOOLEAN) {

		@Override
		public String doSerialize(CmfValue value) {
			return Boolean.toString(value.asBoolean());
		}

		@Override
		public CmfValue doDeserialize(String str) {
			return new CmfValue(Boolean.valueOf(str));
		}

	},
	INTEGER(CmfValueType.INTEGER) {

		@Override
		public String doSerialize(CmfValue value) {
			return String.valueOf(value.asInteger());
		}

		@Override
		public CmfValue doDeserialize(String str) {
			return new CmfValue(Integer.valueOf(str));
		}
	},
	DOUBLE(CmfValueType.DOUBLE) {

		@Override
		public String doSerialize(CmfValue value) {
			return String.format("%d", Double.doubleToRawLongBits(value.asDouble()));
		}

		@Override
		public CmfValue doDeserialize(String str) {
			return new CmfValue(Double.longBitsToDouble(Long.decode(str)));
		}

		@Override
		protected boolean canSerialize(CmfValueType type) {
			switch (type) {
				case INTEGER:
					return true;
				default:
					return super.canSerialize(type);
			}
		}
	},
	STRING(CmfValueType.STRING, true) {

		@Override
		protected boolean canSerialize(CmfValueType type) {
			// This guy is a catch-all
			return true;
		}

	},
	ID(CmfValueType.ID) {

		private final Pattern parser = Pattern.compile("^%ID\\{(.*)\\}%$");

		@Override
		public String doSerialize(CmfValue value) {
			return String.format("%%ID{%s}%%", value.asString());
		}

		@Override
		public CmfValue doDeserialize(String str) throws ParseException {
			Matcher m = this.parser.matcher(str);
			if (!m.matches()) { throw new ParseException(String.format("The string [%s] is not a valid ID string", str),
				0); }
			return new CmfValue(CmfValueType.ID, m.group(1));
		}

		@Override
		protected boolean canSerialize(CmfValueType type) {
			switch (type) {
				case STRING:
				case ID:
				case INTEGER:
					return true;
				default:
					return false;
			}
		}

	},
	TIME(CmfValueType.DATETIME) {

		private final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";

		@Override
		public String doSerialize(CmfValue value) throws ParseException {
			if (value.isNull()) { return null; }
			return DateFormatUtils.format(value.asTime(), this.PATTERN);
		}

		@Override
		public CmfValue doDeserialize(String str) throws ParseException {
			return new CmfValue(DateUtils.parseDate(str, this.PATTERN));
		}

	};

	private CmfValueType type;
	private final boolean supportsEmptyString;

	private CmfValueSerializer(CmfValueType type) {
		this(type, false);
	}

	private CmfValueSerializer(CmfValueType type, boolean supportsEmptyString) {
		this.type = type;
		this.supportsEmptyString = supportsEmptyString;
	}

	protected boolean canSerialize(CmfValueType type) {
		return (this.type == type);
	}

	public final CmfValueType getType() {
		return this.type;
	}

	public final String serialize(CmfValue value) throws ParseException {
		if (value == null) { return null; }
		if (!canSerialize(value.getDataType())) { throw new ParseException(
			String.format("Can't serialize a value of type [%s] as a [%s]", value.getDataType(), name()), 0); }
		return doSerialize(value);
	}

	protected String doSerialize(CmfValue value) throws ParseException {
		return value.asString();
	}

	public final CmfValue deserialize(String str) throws ParseException {
		// If the empty string isn't a valid serialization for this value type, and
		// the string is empty, then just return the NULL value
		// TODO: Should we instead return type.getNull() here?
		if ((str == null) || (!this.supportsEmptyString && StringUtils.isEmpty(str))) { return null; }
		return doDeserialize(str);
	}

	protected CmfValue doDeserialize(String str) throws ParseException {
		return new CmfValue(str);
	}

	private static Map<CmfValueType, CmfValueSerializer> MAP = null;

	public static CmfValueSerializer get(CmfValueType type) {
		synchronized (CmfValueSerializer.class) {
			if (CmfValueSerializer.MAP == null) {
				Map<CmfValueType, CmfValueSerializer> m = new EnumMap<>(CmfValueType.class);
				for (CmfValueSerializer s : CmfValueSerializer.values()) {
					if (m.containsKey(s.type)) { throw new IllegalStateException(
						String.format("Duplicate mapping for data type [%s]", s.type)); }
					m.put(s.type, s);
				}
				CmfValueSerializer.MAP = Tools.freezeMap(m);
			}
			return CmfValueSerializer.MAP.get(type);
		}
	}
}