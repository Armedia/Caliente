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

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.codec.CheckedCodec;

public enum CmfValueSerializer implements CheckedCodec<CmfValue, String, ParseException> {
	//
	BOOLEAN(CmfValue.Type.BOOLEAN) {

		@Override
		public String doSerialize(CmfValue value) {
			return Boolean.toString(value.asBoolean());
		}

		@Override
		public CmfValue doDeserialize(String str) {
			return new CmfValue(Boolean.valueOf(str));
		}
	},
	INTEGER(CmfValue.Type.INTEGER) {

		@Override
		public String doSerialize(CmfValue value) {
			return String.valueOf(value.asInteger());
		}

		@Override
		public CmfValue doDeserialize(String str) {
			return new CmfValue(Integer.valueOf(str));
		}
	},
	DOUBLE(CmfValue.Type.DOUBLE) {

		@Override
		public String doSerialize(CmfValue value) {
			return String.format("%d", Double.doubleToRawLongBits(value.asDouble()));
		}

		@Override
		public CmfValue doDeserialize(String str) {
			return new CmfValue(Double.longBitsToDouble(Long.decode(str)));
		}

		@Override
		protected boolean canSerialize(CmfValue.Type type) {
			switch (type) {
				case INTEGER:
					return true;
				default:
					return super.canSerialize(type);
			}
		}
	},
	STRING(CmfValue.Type.STRING, true) {

		@Override
		protected boolean canSerialize(CmfValue.Type type) {
			// This guy is a catch-all
			return true;
		}

	},
	ID(CmfValue.Type.ID) {

		private final Pattern parser = Pattern.compile("^%ID\\{(.*)\\}%$");

		@Override
		public String doSerialize(CmfValue value) {
			return String.format("%%ID{%s}%%", value.asString());
		}

		@Override
		public CmfValue doDeserialize(String str) throws ParseException {
			Matcher m = this.parser.matcher(str);
			if (!m.matches()) {
				throw new ParseException(String.format("The string [%s] is not a valid ID string", str), 0);
			}
			return new CmfValue(CmfValue.Type.ID, m.group(1));
		}

		@Override
		protected boolean canSerialize(CmfValue.Type type) {
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
	TIME(CmfValue.Type.DATETIME) {

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

	private CmfValue.Type type;
	private final boolean supportsEmptyString;

	private CmfValueSerializer(CmfValue.Type type) {
		this(type, false);
	}

	private CmfValueSerializer(CmfValue.Type type, boolean supportsEmptyString) {
		this.type = type;
		this.supportsEmptyString = supportsEmptyString;
	}

	protected boolean canSerialize(CmfValue.Type type) {
		return (this.type == type);
	}

	public final CmfValue.Type getType() {
		return this.type;
	}

	public final String serialize(CmfValue value) throws ParseException {
		if (value == null) { return null; }
		if (!canSerialize(value.getDataType())) {
			throw new ParseException(
				String.format("Can't serialize a value of type [%s] as a [%s]", value.getDataType(), name()), 0);
		}
		return doSerialize(value);
	}

	protected String doSerialize(CmfValue value) throws ParseException {
		return value.asString();
	}

	@Override
	public String encode(CmfValue v) throws ParseException {
		return serialize(v);
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

	@Override
	public CmfValue decode(String e) throws ParseException {
		return deserialize(e);
	}

	private static Map<CmfValue.Type, CmfValueSerializer> MAP = null;
	static {
		Map<CmfValue.Type, CmfValueSerializer> m = new EnumMap<>(CmfValue.Type.class);
		for (CmfValueSerializer s : CmfValueSerializer.values()) {
			if (m.containsKey(s.type)) {
				throw new IllegalStateException(String.format("Duplicate mapping for data type [%s]", s.type));
			}
			m.put(s.type, s);
		}
		CmfValueSerializer.MAP = Tools.freezeMap(m);
	}

	public static CmfValueSerializer get(CmfValue.Type type) {
		return CmfValueSerializer.MAP.get(type);
	}
}