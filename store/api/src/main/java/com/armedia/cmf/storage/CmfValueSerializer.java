package com.armedia.cmf.storage;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import com.armedia.commons.utilities.Tools;

public enum CmfValueSerializer {
	//
	BOOLEAN(CmfDataType.BOOLEAN) {

		@Override
		public String doSerialize(CmfValue value) {
			return Boolean.toString(value.asBoolean());
		}

		@Override
		public CmfValue doDeserialize(String str) {
			return new CmfValue(Boolean.valueOf(str));
		}

	},
	INTEGER(CmfDataType.INTEGER) {

		@Override
		public String doSerialize(CmfValue value) {
			return String.valueOf(value.asInteger());
		}

		@Override
		public CmfValue doDeserialize(String str) {
			return new CmfValue(Integer.valueOf(str));
		}

	},
	DOUBLE(CmfDataType.DOUBLE) {

		@Override
		public String doSerialize(CmfValue value) {
			return String.format("%d", Double.doubleToRawLongBits(value.asDouble()));
		}

		@Override
		public CmfValue doDeserialize(String str) {
			return new CmfValue(Double.longBitsToDouble(Long.decode(str)));
		}

	},
	STRING(CmfDataType.STRING) {

		@Override
		protected boolean canSerialize(CmfDataType type) {
			// This guy is a catch-all
			return true;
		}

	},
	ID(CmfDataType.ID) {

		private final Pattern parser = Pattern.compile("^%ID\\{(.*)\\}%$");

		@Override
		public String doSerialize(CmfValue value) {
			return String.format("%%ID{%s}%%", value.asString());
		}

		@Override
		public CmfValue doDeserialize(String str) throws ParseException {
			Matcher m = this.parser.matcher(str);
			if (!m.matches()) { throw new ParseException(
				String.format("The string [%s] is not a valid ID string", str), 0); }
			return new CmfValue(CmfDataType.ID, m.group(1));
		}

		@Override
		protected boolean canSerialize(CmfDataType type) {
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
	TIME(CmfDataType.DATETIME) {

		private final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";

		@Override
		public String doSerialize(CmfValue value) throws ParseException {
			return DateFormatUtils.format(value.asTime(), this.PATTERN);
		}

		@Override
		public CmfValue doDeserialize(String str) throws ParseException {
			return new CmfValue(DateUtils.parseDate(str, this.PATTERN));
		}

	};

	private CmfDataType type;

	private CmfValueSerializer(CmfDataType type) {
		this.type = type;
	}

	protected boolean canSerialize(CmfDataType type) {
		return (this.type == type);
	}

	public final CmfDataType getType() {
		return this.type;
	}

	public final String serialize(CmfValue value) throws ParseException {
		if (value == null) { return null; }
		if (!canSerialize(value.getDataType())) { throw new ParseException(String.format(
			"Can't serialize a value of type [%s] as a [%s]", value.getDataType(), name()), 0); }
		return doSerialize(value);
	}

	protected String doSerialize(CmfValue value) throws ParseException {
		return value.asString();
	}

	public final CmfValue deserialize(String str) throws ParseException {
		if (str == null) { return null; }
		return doDeserialize(str);
	}

	protected CmfValue doDeserialize(String str) throws ParseException {
		return new CmfValue(str);
	}

	private static Map<CmfDataType, CmfValueSerializer> MAP = null;

	public static CmfValueSerializer get(CmfDataType type) {
		synchronized (CmfValueSerializer.class) {
			if (CmfValueSerializer.MAP == null) {
				Map<CmfDataType, CmfValueSerializer> m = new EnumMap<CmfDataType, CmfValueSerializer>(CmfDataType.class);
				for (CmfValueSerializer s : CmfValueSerializer.values()) {
					if (m.containsKey(s.type)) { throw new IllegalStateException(String.format(
						"Duplicate mapping for data type [%s]", s.type)); }
					m.put(s.type, s);
				}
				CmfValueSerializer.MAP = Tools.freezeMap(m);
			}
			return CmfValueSerializer.MAP.get(type);
		}
	}
}