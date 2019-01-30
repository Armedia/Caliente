package com.armedia.caliente.store;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public enum CmfValueType {
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
					String.format("Failed to convert value [%s] of type [%s] into a DATETIME value", value.asString(),
						value.getDataType()),
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

	private CmfValueType() {
		this(null);
	}

	private CmfValueType(String abbreviation) {
		this.abbrev = StringUtils.lowerCase(Tools.coalesce(abbreviation, name()));
	}

	private static final Map<CmfValueType, CmfValue> NULL;
	private static final Map<String, CmfValueType> ABBREV;

	static {
		Map<CmfValueType, CmfValue> nvl = new EnumMap<>(CmfValueType.class);
		Map<String, CmfValueType> abb = new TreeMap<>();
		for (CmfValueType t : CmfValueType.values()) {
			CmfValueType o = abb.put(t.abbrev, t);
			if (o != null) {
				throw new RuntimeException(
					String.format("ERROR: The CmfValueType values %s and %s share the same abbreviation [%s]", t.name(),
						o.name(), t.abbrev));
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
		return CmfValueType.NULL.get(this);
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

	protected abstract Object doGetValue(CmfValue value) throws Exception;

	public static CmfValueType decode(String value) {
		if (value == null) { return null; }
		try {
			return CmfValueType.valueOf(StringUtils.upperCase(value));
		} catch (final IllegalArgumentException e) {
			// Maybe an abbreviation?
			CmfValueType t = CmfValueType.ABBREV.get(StringUtils.lowerCase(value));
			if (t != null) { return t; }
			throw e;
		}
	}
}