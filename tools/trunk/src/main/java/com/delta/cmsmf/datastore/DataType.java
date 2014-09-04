package com.delta.cmsmf.datastore;

import java.util.Date;

import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.DfValue;
import com.documentum.fc.common.IDfValue;

public enum DataType {

	DF_BOOLEAN(IDfValue.DF_BOOLEAN) {
		private final IDfValue clearValue = DfValueFactory.newBooleanValue(false);

		@Override
		public IDfValue doDecode(String value) {
			return new DfValue(value, IDfValue.DF_BOOLEAN);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asBoolean();
		}

		@Override
		public IDfValue getClearingValue() {
			return this.clearValue;
		}
	},
	DF_INTEGER(IDfValue.DF_INTEGER) {
		private final IDfValue clearValue = DfValueFactory.newIntValue(0);

		@Override
		public IDfValue doDecode(String value) {
			return new DfValue(value, IDfValue.DF_INTEGER);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asInteger();
		}

		@Override
		public IDfValue getClearingValue() {
			return this.clearValue;
		}
	},
	DF_STRING(IDfValue.DF_STRING) {
		private final IDfValue clearValue = DfValueFactory.newStringValue("");

		@Override
		public IDfValue doDecode(String value) {
			return new DfValue(value, IDfValue.DF_STRING);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asString();
		}

		@Override
		public IDfValue getClearingValue() {
			return this.clearValue;
		}
	},
	DF_ID(IDfValue.DF_ID) {
		private final IDfValue clearValue = DfValueFactory.newIdValue(DfId.DF_NULLID);

		@Override
		public String doEncode(IDfValue value) {
			return value.asId().getId();
		}

		@Override
		public IDfValue doDecode(String value) {
			return new DfValue(value, IDfValue.DF_ID);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asId();
		}

		@Override
		public IDfValue getClearingValue() {
			return this.clearValue;
		}
	},
	DF_TIME(IDfValue.DF_TIME) {
		private final String nullDate = "{NULL_DATE}";
		private final IDfValue nullValue = new DfValue(DfTime.DF_NULLDATE);
		private final IDfValue clearValue = this.nullValue;

		@Override
		protected boolean isNullEncoding(String value) {
			return ((value == null) || this.nullDate.equals(value));
		}

		@Override
		protected String getNullEncoding() {
			return this.nullDate;
		}

		@Override
		protected boolean isNullValue(IDfValue value) {
			return ((value == null) || value.asTime().isNullDate());
		}

		@Override
		protected IDfValue getNullValue() {
			return this.nullValue;
		}

		@Override
		public String doEncode(IDfValue value) {
			return String.format("%d", value.asTime().getDate().getTime());
		}

		@Override
		public IDfValue doDecode(String value) {
			if (this.nullDate.equalsIgnoreCase(value)) { return getNullValue(); }
			return new DfValue(new DfTime(new Date(Long.parseLong(value))));
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asTime();
		}

		@Override
		public IDfValue getClearingValue() {
			return this.clearValue;
		}
	},
	DF_DOUBLE(IDfValue.DF_DOUBLE) {
		private final IDfValue clearValue = DfValueFactory.newDoubleValue(0.0);

		@Override
		public String doEncode(IDfValue value) {
			return Double.toHexString(value.asDouble());
		}

		@Override
		public IDfValue doDecode(String value) {
			return new DfValue(value, IDfValue.DF_DOUBLE);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asDouble();
		}

		@Override
		public IDfValue getClearingValue() {
			return this.clearValue;
		}
	},
	DF_UNDEFINED(IDfValue.DF_UNDEFINED) {
		private <T> T fail() {
			throw new RuntimeException("Can't handle DF_UNDEFINED");
		}

		@Override
		public String doEncode(IDfValue value) {
			return fail();
		}

		@Override
		public IDfValue doDecode(String value) {
			return fail();
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return fail();
		}

		@Override
		public IDfValue getClearingValue() {
			return fail();
		}
	};

	private final int dfConstant;

	private DataType(int dfConstant) {
		this.dfConstant = dfConstant;
	}

	public final int getDfConstant() {
		return this.dfConstant;
	}

	public abstract IDfValue getClearingValue();

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