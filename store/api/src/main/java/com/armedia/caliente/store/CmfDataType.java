package com.armedia.caliente.store;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

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

	public static CmfDataType decodeString(String str) {
		if (str == null) { throw new NullPointerException("Must provide a valid string to decode"); }
		try {
			return CmfDataType.valueOf(str);
		} catch (IllegalArgumentException e) {
			for (CmfTypeDecoder d : CmfTypeDecoder.DECODERS) {
				CmfDataType ret = d.translateDataType(str);
				if (ret != null) { return ret; }
			}
			throw new IllegalArgumentException(
				String.format("The string [%s] could not be translated to a CmfDataType", str));
		}
	}
}