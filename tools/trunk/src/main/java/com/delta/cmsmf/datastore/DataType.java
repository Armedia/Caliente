package com.delta.cmsmf.datastore;

import java.util.Date;

import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.DfValue;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;
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
			IDfId id = value.asId();
			return (id.isNull() ? null : id.getId());
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
		@Override
		public String doEncode(IDfValue value) {
			IDfTime t = value.asTime();
			return String.format("%d", t.getDate().getTime());
		}

		@Override
		public IDfValue doDecode(String value) {
			return new DfValue(new DfTime(new Date(Long.parseLong(value))));
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

	public final String encode(IDfValue value) {
		if (value == null) { return null; }
		return doEncode(value);
	}

	protected String doEncode(IDfValue value) {
		return value.asString();
	}

	public final IDfValue decode(String value) {
		if (value == null) { return null; }
		return doDecode(value);
	}

	protected abstract IDfValue doDecode(String value);

	public final Object getValue(IDfValue value) {
		if (value == null) { return null; }
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