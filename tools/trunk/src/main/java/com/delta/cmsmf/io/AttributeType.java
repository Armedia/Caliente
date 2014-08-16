package com.delta.cmsmf.io;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import com.documentum.fc.common.IDfValue;

@XmlType(name = "attribute_type.t")
@XmlEnum
public enum AttributeType {
	//
	BOOLEAN(IDfValue.DF_BOOLEAN) {
		@Override
		public Serializable getValue(IDfValue value) {
			if (value == null) { return null; }
			return value.asBoolean();
		}

		@Override
		public Serializable getValue(String value) {
			if (value == null) { return null; }
			return Boolean.valueOf(value);
		}
	},
	INTEGER(IDfValue.DF_INTEGER) {
		@Override
		public Serializable getValue(IDfValue value) {
			if (value == null) { return null; }
			return value.asInteger();
		}

		@Override
		public Serializable getValue(String value) {
			if (value == null) { return null; }
			return Integer.valueOf(value);
		}
	},
	STRING(IDfValue.DF_STRING) {
		@Override
		public Serializable getValue(IDfValue value) {
			if (value == null) { return null; }
			return value.asString();
		}

		@Override
		public Serializable getValue(String value) {
			return value;
		}
	},
	ID(IDfValue.DF_ID) {
		@Override
		public Serializable getValue(IDfValue value) {
			if (value == null) { return null; }
			return value.asId().getId();
		}

		@Override
		public Serializable getValue(String value) {
			return value;
		}
	},
	TEMPORAL(IDfValue.DF_TIME) {
		@Override
		public Serializable getValue(IDfValue value) {
			if (value.asId().isNull()) { return null; }
			if (value.asTime().isNullDate()) { return null; }
			return value.asTime().getDate();
		}

		@Override
		public Serializable getValue(String value) {
			if (value == null) { return null; }
			// TODO: Parse the date from the same format we wrote it out in
			return Boolean.valueOf(value);
		}

		@Override
		public String getValue(Serializable value) {
			if (value == null) { return null; }
			// TODO: Output the date in the same format we mean to parse it in
			return value.toString();
		}
	},
	DOUBLE(IDfValue.DF_DOUBLE) {
		@Override
		public Serializable getValue(IDfValue value) {
			if (value == null) { return null; }
			return value.asDouble();
		}

		@Override
		public Serializable getValue(String value) {
			if (value == null) { return null; }
			return Double.valueOf(value);
		}
	},
	UNDEFINED(IDfValue.DF_UNDEFINED) {
		@Override
		public Serializable getValue(IDfValue value) {
			if (value == null) { return null; }
			return value.asString();
		}

		@Override
		public Serializable getValue(String value) {
			return value;
		}
	};

	private final int dmValue;

	private AttributeType(int dmValue) {
		this.dmValue = dmValue;
	}

	public abstract Serializable getValue(IDfValue value);

	public abstract Serializable getValue(String value);

	public String getValue(Serializable value) {
		if (value == null) { return null; }
		return value.toString();
	}

	public static AttributeType decode(int dmValue) {
		for (AttributeType t : AttributeType.values()) {
			if (t.dmValue == dmValue) { return t; }
		}
		throw new IllegalArgumentException(String.format("Unsupported attribute type %d", dmValue));
	}
}