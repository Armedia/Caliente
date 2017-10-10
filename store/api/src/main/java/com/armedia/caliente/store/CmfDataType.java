package com.armedia.caliente.store;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

import com.armedia.commons.utilities.Tools;

public enum CmfDataType {
	//
	BOOLEAN {
		@Override
		protected Object doGetValue(CmfValue value) {
			return value.asBoolean();
		}
	},
	INTEGER {
		@Override
		protected Object doGetValue(CmfValue value) {
			return value.asInteger();
		}
	},
	DOUBLE {
		@Override
		protected Object doGetValue(CmfValue value) {
			return value.asDouble();
		}
	},
	STRING {
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
	DATETIME {
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
	BASE64_BINARY {
		@Override
		protected Object doGetValue(CmfValue value) {
			return value.asBinary();
		}
	},
	OTHER {
		@Override
		protected Object doGetValue(CmfValue value) {
			throw new UnsupportedOperationException("Values of type OTHER can't be converted to");
		}
	},
	//
	;

	private static final Map<CmfDataType, CmfValue> NULL;

	static {
		Map<CmfDataType, CmfValue> nvl = new EnumMap<>(CmfDataType.class);
		for (CmfDataType t : CmfDataType.values()) {
			try {
				nvl.put(t, new CmfValue(t, null));
			} catch (ParseException e) {
				throw new RuntimeException(
					String.format("Failed to create a CMF value with a null value for type [%s]", t), e);
			}
		}
		NULL = Tools.freezeMap(nvl);
	}

	public final CmfValue getNull() {
		return CmfDataType.NULL.get(this);
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
}