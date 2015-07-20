package com.armedia.cmf.engine.documentum;

import java.text.ParseException;
import java.util.Date;

import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueCodec;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.DfValue;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

public enum DctmDataType implements CmfValueCodec<IDfValue> {
	DF_BOOLEAN(CmfDataType.BOOLEAN, IDfValue.DF_BOOLEAN) {
		private final String nullEncoding = String.valueOf(false);
		private final IDfValue nullValue = new DfValue(this.nullEncoding, IDfValue.DF_BOOLEAN);

		@Override
		public IDfValue doDecode(CmfValue value) {
			return new DfValue(Boolean.toString(value.asBoolean()), IDfValue.DF_BOOLEAN);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asBoolean();
		}

		@Override
		public IDfValue getNull() {
			return this.nullValue;
		}

		@Override
		public boolean isNull(IDfValue v) {
			return (v == null);
		}
	},
	DF_INTEGER(CmfDataType.INTEGER, IDfValue.DF_INTEGER) {
		private final String nullEncoding = String.valueOf(0);
		private final IDfValue nullValue = new DfValue(this.nullEncoding, IDfValue.DF_INTEGER);

		@Override
		public IDfValue doDecode(CmfValue value) {
			return new DfValue(String.valueOf(value.asInteger()), IDfValue.DF_INTEGER);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asInteger();
		}

		@Override
		public IDfValue getNull() {
			return this.nullValue;
		}

		@Override
		public boolean isNull(IDfValue v) {
			return (v == null);
		}
	},
	DF_STRING(CmfDataType.STRING, IDfValue.DF_STRING) {
		private final String nullEncoding = "";
		private final IDfValue nullValue = new DfValue(this.nullEncoding, IDfValue.DF_STRING);

		@Override
		public IDfValue doDecode(CmfValue value) {
			return new DfValue(value.asString(), IDfValue.DF_STRING);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asString();
		}

		@Override
		public IDfValue getNull() {
			return this.nullValue;
		}
	},
	DF_ID(CmfDataType.ID, IDfValue.DF_ID) {
		private final String nullEncoding = DfId.DF_NULLID_STR;
		private final IDfValue nullValue = new DfValue(this.nullEncoding, IDfValue.DF_ID);

		@Override
		public CmfValue doEncode(IDfValue value) {
			try {
				return new CmfValue(CmfDataType.ID, value.asId().getId());
			} catch (ParseException e) {
				throw new RuntimeException("Unexpected parsing exception", e);
			}
		}

		@Override
		public IDfValue doDecode(CmfValue value) {
			return new DfValue(value.asString(), IDfValue.DF_ID);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asId();
		}

		@Override
		public IDfValue getNull() {
			return this.nullValue;
		}
	},
	DF_TIME(CmfDataType.DATETIME, IDfValue.DF_TIME) {
		private final IDfValue nullValue = new DfValue(DfTime.DF_NULLDATE);

		@Override
		public CmfValue doEncode(IDfValue value) {
			IDfTime t = value.asTime();
			if (t.isNullDate() || !t.isValid()) {
				try {
					return new CmfValue(CmfDataType.DATETIME, null);
				} catch (ParseException e) {
					// Not going to happen...
					throw new RuntimeException("Unexpected parse exception", e);
				}
			}
			return new CmfValue(t.getDate());
		}

		@Override
		public IDfValue doDecode(CmfValue value) {
			try {
				return new DfValue(new DfTime(value.asTime()));
			} catch (ParseException e) {
				throw new RuntimeException(String.format("Failed to decode the value [%s] as a Date value",
					value.asString()));
			}
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asTime();
		}

		@Override
		public IDfValue getNull() {
			return this.nullValue;
		}
	},
	DF_DOUBLE(CmfDataType.DOUBLE, IDfValue.DF_DOUBLE) {
		private final String nullEncoding = Double.toHexString(0.0);
		private final IDfValue nullValue = new DfValue(this.nullEncoding, IDfValue.DF_DOUBLE);

		@Override
		public CmfValue doEncode(IDfValue value) {
			return new CmfValue(value.asDouble());
		}

		@Override
		public IDfValue doDecode(CmfValue value) {
			return new DfValue(Double.toHexString(value.asDouble()), IDfValue.DF_DOUBLE);
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return value.asDouble();
		}

		@Override
		public IDfValue getNull() {
			return this.nullValue;
		}
	},
	DF_UNDEFINED(null, IDfValue.DF_UNDEFINED) {
		private <T> T fail() {
			throw new UnsupportedOperationException("Can't handle DF_UNDEFINED");
		}

		@Override
		public CmfValue doEncode(IDfValue value) {
			return fail();
		}

		@Override
		public IDfValue doDecode(CmfValue value) {
			return fail();
		}

		@Override
		protected Object doGetValue(IDfValue value) {
			return fail();
		}

		@Override
		public IDfValue getNull() {
			return fail();
		}
	};

	private final CmfDataType type;
	private final int dfConstant;

	private DctmDataType(CmfDataType type, int dfConstant) {
		this.type = type;
		this.dfConstant = dfConstant;
	}

	public final CmfDataType getStoredType() {
		return this.type;
	}

	public final int getDfConstant() {
		return this.dfConstant;
	}

	/**
	 * <p>
	 * Returns the null-equivalent value for this data type. This will <b>never</b> return
	 * {@code null}. The strict definition of this method is that the invocation
	 * {@code isNull(getNull())} <b><i>must</i></b> return {@code true}.
	 * </p>
	 *
	 * @return the null-equivalent value for this data type
	 */
	@Override
	public abstract IDfValue getNull();

	/**
	 * <p>
	 * Returns {@code true} if the given value is the null-equivalent value for this data type. The
	 * strict definition of this method is that the invocation {@code isNull(getNull())}
	 * <b><i>must</i></b> return {@code true}.
	 * </p>
	 *
	 * @return {@code true} if the given value is the null-equivalent value for this data type
	 */
	@Override
	public boolean isNull(IDfValue v) {
		return (v == null);
	}

	/**
	 * <p>
	 * Encode the value into a {@link CmfValue}, such that for a given value {@code A}, invoking
	 * {@link #decodeValue(CmfValue)} on that encoded CmfValue will result in a value {@code B},
	 * such that {@code A.equals(B)} returns {@code true}.
	 * </p>
	 *
	 * @param value
	 * @return the string-encoded value
	 */
	@Override
	public final CmfValue encodeValue(IDfValue value) {
		if (value == null) {
			value = getNull();
		}
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
	protected CmfValue doEncode(IDfValue value) {
		switch (this.type) {
			case BOOLEAN:
				return new CmfValue(value.asBoolean());
			case DOUBLE:
				return new CmfValue(value.asDouble());
			case ID:
			case INTEGER:
				return new CmfValue(value.asInteger());
			case STRING:
				return new CmfValue(value.asString());
			case DATETIME:
				IDfTime t = value.asTime();
				Date d = null;
				if (t.isNullDate()) {

				} else {
					d = t.getDate();
				}
				return new CmfValue(d);
			default:
				break;
		}
		throw new IllegalArgumentException(String.format("Unsupported conversion type: [%s]", this.type));
	}

	/**
	 * <p>
	 * Decode the string into an {@link IDfValue}, such that for a given string {@code A}, invoking
	 * {@link #encodeValue(IDfValue)} on that decoded {@link IDfValue} will result in a string
	 * {@code B}, such that {@code A.equals(B)} returns {@code true}.
	 * </p>
	 * <p>
	 * The exception to this rule is the value {@code null}: this will get encoded into an
	 * {@link IDfValue} instance as specified by {@link #getNull()}.
	 * </p>
	 *
	 * @param value
	 * @return the string-encoded value
	 */
	@Override
	public final IDfValue decodeValue(CmfValue value) {
		if ((value == null) || value.isNull()) { return getNull(); }
		return doDecode(value);
	}

	protected abstract IDfValue doDecode(CmfValue value);

	public final Object getValue(IDfValue value) {
		if (value == null) {
			value = getNull();
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
		return DctmDataType.fromDataType(attribute.getDataType());
	}

	public static DctmDataType fromDataType(int dataType) {
		// We do this just to be safe, but we could also use the constant as an array index
		switch (dataType) {
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
				throw new IllegalArgumentException(String.format("Unsupported IDfValue constant [%d]", dataType));
		}
	}
}