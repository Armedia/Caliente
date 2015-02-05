package com.armedia.cmf.storage;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import com.armedia.commons.utilities.Tools;

public enum StoredValueSerializer {
	//
	BOOLEAN(StoredDataType.BOOLEAN) {

		@Override
		public String doSerialize(StoredValue value) {
			return Boolean.toString(value.asBoolean());
		}

		@Override
		public StoredValue doDeserialize(String str) {
			return new StoredValue(Boolean.valueOf(str));
		}

	},
	INTEGER(StoredDataType.INTEGER) {

		@Override
		public String doSerialize(StoredValue value) {
			return String.valueOf(value.asInteger());
		}

		@Override
		public StoredValue doDeserialize(String str) {
			return new StoredValue(Integer.valueOf(str));
		}

	},
	DOUBLE(StoredDataType.DOUBLE) {

		@Override
		public String doSerialize(StoredValue value) {
			long l = Double.doubleToRawLongBits(value.asDouble());
			return String.format("%016x", l);
		}

		@Override
		public StoredValue doDeserialize(String str) {
			return new StoredValue(Double.longBitsToDouble(Long.parseLong(str, 16)));
		}

	},
	STRING(StoredDataType.STRING) {

		@Override
		protected boolean canSerialize(StoredDataType type) {
			// This guy is a catch-all
			return true;
		}

	},
	ID(StoredDataType.ID) {

		private final Pattern parser = Pattern.compile("^%ID\\{(.*)\\}%$");

		@Override
		public String doSerialize(StoredValue value) {
			return String.format("%%ID{%s}%%", value.asString());
		}

		@Override
		public StoredValue doDeserialize(String str) throws ParseException {
			Matcher m = this.parser.matcher(str);
			if (!m.matches()) { throw new ParseException(
				String.format("The string [%s] is not a valid ID string", str), 0); }
			return new StoredValue(StoredDataType.ID, m.group(1));
		}

	},
	TIME(StoredDataType.TIME) {

		private final FastDateFormat format = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;

		@Override
		public String doSerialize(StoredValue value) throws ParseException {
			return this.format.format(value.asTime());
		}

		@Override
		public StoredValue doDeserialize(String str) throws ParseException {
			return new StoredValue(DateUtils.parseDate(str, this.format.getPattern()));
		}

	};

	private StoredDataType type;

	private StoredValueSerializer(StoredDataType type) {
		this.type = type;
	}

	protected boolean canSerialize(StoredDataType type) {
		return (this.type == type);
	}

	public final String serialize(StoredValue value) throws ParseException {
		if (value == null) { return null; }
		if (!canSerialize(value.getDataType())) { throw new ParseException(String.format(
			"Can't serialize a value of type [%s] as a [%s]", value.getDataType(), name()), 0); }
		return doSerialize(value);
	}

	protected String doSerialize(StoredValue value) throws ParseException {
		return value.asString();
	}

	public final StoredValue deserialize(String str) throws ParseException {
		if (str == null) { return null; }
		return doDeserialize(str);
	}

	protected StoredValue doDeserialize(String str) throws ParseException {
		return new StoredValue(str);
	}

	private static Map<StoredDataType, StoredValueSerializer> MAP = null;

	public static StoredValueSerializer get(StoredDataType type) {
		synchronized (StoredValueSerializer.class) {
			if (StoredValueSerializer.MAP == null) {
				Map<StoredDataType, StoredValueSerializer> m = new EnumMap<StoredDataType, StoredValueSerializer>(
					StoredDataType.class);
				for (StoredValueSerializer s : StoredValueSerializer.values()) {
					if (m.containsKey(s.type)) { throw new IllegalStateException(String.format(
						"Duplicate mapping for data type [%s]", s.type)); }
					m.put(s.type, s);
				}
				StoredValueSerializer.MAP = Tools.freezeMap(m);
			}
			return StoredValueSerializer.MAP.get(type);
		}
	}
}