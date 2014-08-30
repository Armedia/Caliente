package com.delta.cmsmf.datastore;

import java.util.Date;

import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.DfValue;
import com.documentum.fc.common.IDfValue;

public enum DataType {

	DF_BOOLEAN(IDfValue.DF_BOOLEAN) {
		@Override
		public IDfValue doDecode(String value) {
			return new DfValue(Boolean.valueOf(value), IDfValue.DF_BOOLEAN);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asBoolean();
		}
	},
	DF_INTEGER(IDfValue.DF_INTEGER) {
		@Override
		public IDfValue doDecode(String value) {
			return new DfValue(Integer.parseInt(value), IDfValue.DF_INTEGER);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asInteger();
		}
	},
	DF_STRING(IDfValue.DF_STRING) {
		@Override
		public IDfValue doDecode(String value) {
			return new DfValue(value, IDfValue.DF_STRING);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asString();
		}
	},
	DF_ID(IDfValue.DF_ID) {
		@Override
		public String doEncode(IDfValue value) {
			return value.asId().getId();
		}

		@Override
		public IDfValue doDecode(String value) {
			return new DfValue(new DfId(value), IDfValue.DF_ID);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asId();
		}
	},
	DF_TIME(IDfValue.DF_TIME) {
		private final String NULL_DATE = "{NULL_DATE}";
		private final IDfValue NULL_VALUE = new DfValue(new DfTime((Date) null));

		@Override
		protected boolean isNullEncoding(String value) {
			return ((value == null) || this.NULL_DATE.equals(value));
		}

		@Override
		protected String getNullEncoding() {
			return this.NULL_DATE;
		}

		@Override
		protected boolean isNullValue(IDfValue value) {
			return ((value == null) || value.asTime().isNullDate());
		}

		@Override
		protected IDfValue getNullValue() {
			return this.NULL_VALUE;
		}

		@Override
		public String doEncode(IDfValue value) {
			return String.format("%d", value.asTime().getDate().getTime());
		}

		@Override
		public IDfValue doDecode(String value) {
			Date date = null;
			if (!this.NULL_DATE.equalsIgnoreCase(value)) {
				date = new Date(Long.parseLong(value));
			}
			return new DfValue(new DfTime(date));
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asTime();
		}
	},
	DF_DOUBLE(IDfValue.DF_DOUBLE) {
		@Override
		public String doEncode(IDfValue value) {
			return String.format("%d", Double.doubleToRawLongBits(value.asDouble()));
		}

		@Override
		public IDfValue doDecode(String value) {
			return new DfValue(Long.parseLong(value), IDfValue.DF_DOUBLE);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asDouble();
		}
	},
	DF_UNDEFINED(IDfValue.DF_UNDEFINED) {
		@Override
		public String doEncode(IDfValue value) {
			throw new RuntimeException("Can't handle DF_UNDEFINED");
		}

		@Override
		public IDfValue doDecode(String value) {
			throw new RuntimeException("Can't handle DF_UNDEFINED");
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			throw new RuntimeException("Can't handle DF_UNDEFINED");
		}
	};

	private final int dfConstant;

	private DataType(int dfConstant) {
		this.dfConstant = dfConstant;
	}

	public final int getDfConstant() {
		return this.dfConstant;
	}

	protected String getNullEncoding() {
		return null;
	}

	protected boolean isNullValue(IDfValue value) {
		return (value == null);
	}

	protected IDfValue getNullValue() {
		return null;
	}

	protected boolean isNullEncoding(String value) {
		return (value == null);
	}

	public final String encode(IDfValue value) {
		if (isNullValue(value)) { return getNullEncoding(); }
		return doEncode(value);
	}

	protected String doEncode(IDfValue value) {
		return value.asString();
	}

	public final IDfValue decode(String value) {
		if (isNullEncoding(value)) { return getNullValue(); }
		return doDecode(value);
	}

	protected abstract IDfValue doDecode(String value);

	public final Object getValue(IDfValue value) {
		if (isNullValue(value)) { return getNullValue(); }
		return doGetValue(value);
	}

	protected abstract Object doGetValue(IDfValue value);

	public static DataType fromDfConstant(int constant) {
		// We do this just to be safe, but we could also use the constant as an array index
		switch (constant) {
			case IDfValue.DF_BOOLEAN:
				return DF_BOOLEAN;
			case IDfValue.DF_INTEGER:
				return DF_INTEGER;
			case IDfValue.DF_STRING:
				return DF_STRING;
			case IDfValue.DF_ID:
				return DF_ID;
			case IDfValue.DF_TIME:
				return DF_TIME;
			case IDfValue.DF_DOUBLE:
				return DF_DOUBLE;
			case IDfValue.DF_UNDEFINED:
				return DF_UNDEFINED;
			default:
				throw new IllegalArgumentException(String.format("Unsupported IDfValue constant [%d]"));
		}
	}
}