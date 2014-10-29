package com.armedia.cmf.documentum;

import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredValueCodec;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.DfValue;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

public enum DctmDataType implements StoredValueCodec<IDfValue> {
	DF_BOOLEAN(StoredDataType.BOOLEAN, IDfValue.DF_BOOLEAN) {
		private final String nullEncoding = String.valueOf(false);
		private final IDfValue nullValue = new DfValue(this.nullEncoding, IDfValue.DF_BOOLEAN);

		@Override
		public IDfValue doDecode(String value) {
			return new DfValue(value, IDfValue.DF_BOOLEAN);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asBoolean();
		}

		@Override
		public IDfValue getNullValue() {
			return this.nullValue;
		}

		@Override
		public String getNullEncoding() {
			return this.nullEncoding;
		}
	},
	DF_INTEGER(StoredDataType.INTEGER, IDfValue.DF_INTEGER) {
		private final String nullEncoding = String.valueOf(0);
		private final IDfValue nullValue = new DfValue(this.nullEncoding, IDfValue.DF_INTEGER);

		@Override
		public IDfValue doDecode(String value) {
			return new DfValue(value, IDfValue.DF_INTEGER);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asInteger();
		}

		@Override
		public IDfValue getNullValue() {
			return this.nullValue;
		}

		@Override
		public String getNullEncoding() {
			return this.nullEncoding;
		}
	},
	DF_STRING(StoredDataType.STRING, IDfValue.DF_STRING) {
		private final String nullEncoding = "";
		private final IDfValue nullValue = new DfValue(this.nullEncoding, IDfValue.DF_STRING);

		@Override
		public IDfValue doDecode(String value) {
			return new DfValue(value, IDfValue.DF_STRING);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asString();
		}

		@Override
		public IDfValue getNullValue() {
			return this.nullValue;
		}

		@Override
		public String getNullEncoding() {
			return this.nullEncoding;
		}
	},
	DF_ID(StoredDataType.ID, IDfValue.DF_ID) {
		private final String nullEncoding = DfId.DF_NULLID_STR;
		private final IDfValue nullValue = new DfValue(this.nullEncoding, IDfValue.DF_ID);

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
		public IDfValue getNullValue() {
			return this.nullValue;
		}

		@Override
		public String getNullEncoding() {
			return this.nullEncoding;
		}
	},
	DF_TIME(StoredDataType.TIME, IDfValue.DF_TIME) {
		private final IDfValue nullValue = new DfValue(DfTime.DF_NULLDATE);
		private final String nullDate = this.nullValue.asString();
		private final String timePattern = IDfTime.DF_TIME_PATTERN26;

		@Override
		public String doEncode(IDfValue value) {
			if (value.asTime().isNullDate()) { return this.nullDate; }
			return value.asTime().asString(this.timePattern);
		}

		@Override
		public IDfValue doDecode(String value) {
			if (this.nullDate.equals(value)) { return getNullValue(); }
			return new DfValue(new DfTime(value, this.timePattern));
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asTime();
		}

		@Override
		public IDfValue getNullValue() {
			return this.nullValue;
		}

		@Override
		public String getNullEncoding() {
			return this.nullDate;
		}
	},
	DF_DOUBLE(StoredDataType.DOUBLE, IDfValue.DF_DOUBLE) {
		private final String nullEncoding = Double.toHexString(0.0);
		private final IDfValue nullValue = new DfValue(this.nullEncoding, IDfValue.DF_DOUBLE);

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
		public IDfValue getNullValue() {
			return this.nullValue;
		}

		@Override
		public String getNullEncoding() {
			return this.nullEncoding;
		}
	},
	DF_UNDEFINED(null, IDfValue.DF_UNDEFINED) {
		private <T> T fail() {
			throw new UnsupportedOperationException("Can't handle DF_UNDEFINED");
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
		public IDfValue getNullValue() {
			return fail();
		}

		@Override
		public String getNullEncoding() {
			return fail();
		}
	};

	private final StoredDataType type;
	private final int dfConstant;

	private DctmDataType(StoredDataType type, int dfConstant) {
		this.type = type;
		this.dfConstant = dfConstant;
	}

	public final StoredDataType getStoredType() {
		return this.type;
	}

	public final int getDfConstant() {
		return this.dfConstant;
	}

	/**
	 * <p>
	 * Returns the string-form null-equivalent value for this data type. This will <b>never</b>
	 * return {@code null}.
	 *
	 * @return the null-equivalent encoding for this data type
	 */
	public abstract String getNullEncoding();

	/**
	 * <p>
	 * Returns the null-equivalent value for this data type. This will <b>never</b> return
	 * {@code null}.
	 * </p>
	 *
	 * @return the null-equivalent value for this data type
	 */
	public abstract IDfValue getNullValue();

	/**
	 * <p>
	 * Encode the value into a string, such that for a given value {@code A}, invoking
	 * {@link #decodeValue(String)} on that encoded string will result in a value {@code B}, such
	 * that {@code A.equals(B)} returns {@code true}.
	 * </p>
	 *
	 * @param value
	 * @return the string-encoded value
	 */
	@Override
	public final String encodeValue(IDfValue value) {
		if (value == null) { return getNullEncoding(); }
		return doEncode(value);
	}

	/**
	 * <p>
	 * Perform the actual encoding
	 * </p>
	 *
	 * @param value
	 * @return the encoded value
	 */
	protected String doEncode(IDfValue value) {
		return value.asString();
	}

	/**
	 * <p>
	 * Decode the string into an {@link IDfValue}, such that for a given string {@code A}, invoking
	 * {@link #encodeValue(IDfValue)} on that decoded {@link IDfValue} will result in a string
	 * {@code B}, such that {@code A.equals(B)} returns {@code true}.
	 * </p>
	 * <p>
	 * The exception to this rule is the value {@code null}: this will get encoded into an
	 * {@link IDfValue} instance as specified by {@link #getNullValue()}.
	 * </p>
	 *
	 * @param value
	 * @return the string-encoded value
	 */
	@Override
	public final IDfValue decodeValue(String value) {
		if (value == null) { return getNullValue(); }
		return doDecode(value);
	}

	protected abstract IDfValue doDecode(String value);

	public final Object getValue(IDfValue value) {
		if (value == null) {
			value = getNullValue();
		}
		return doGetValue(value);
	}

	protected abstract Object doGetValue(IDfValue value);

	/*
	 * public static CmsDataType fromDfConstant(int constant) { }
	 */

	public static DctmDataType fromAttribute(IDfAttr attribute) {
		if (attribute == null) { throw new IllegalArgumentException(
			"Must provide an attribute to decode the data type from"); }
		// We do this just to be safe, but we could also use the constant as an array index
		switch (attribute.getDataType()) {
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