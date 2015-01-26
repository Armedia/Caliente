package com.armedia.cmf.storage;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import com.armedia.commons.utilities.Tools;

public final class StoredValue {

	private final StoredDataType type;
	private final Object value;

	public StoredValue(int value) {
		this(StoredDataType.INTEGER, value);
	}

	public StoredValue(boolean value) {
		this(StoredDataType.BOOLEAN, value);
	}

	public StoredValue(double value) {
		this(StoredDataType.DOUBLE, value);
	}

	public StoredValue(String value) {
		this(StoredDataType.STRING, value);
	}

	public StoredValue(Date value) {
		this(StoredDataType.TIME, value);
	}

	public StoredValue(StoredDataType type, Object value) {
		this.type = type;
		this.value = value;
	}

	public String asString() {
		return Tools.toString(this.value);
	}

	public String asId() {
		return asString();
	}

	public int asInteger() {
		if (this.value == null) { return 0; }
		if (this.value instanceof Number) { return Number.class.cast(this.value).intValue(); }
		return Integer.valueOf(this.value.toString());
	}

	public boolean asBoolean() {
		if (this.value == null) { return false; }
		if (this.value instanceof Boolean) { return Boolean.class.cast(this.value).booleanValue(); }
		return Boolean.valueOf(this.value.toString());
	}

	public double asDouble() {
		if (this.value == null) { return 0; }
		if (this.value instanceof Number) { return Number.class.cast(this.value).doubleValue(); }
		return Double.valueOf(this.value.toString());
	}

	public Date asTime() throws ParseException {
		if (this.value == null) { return null; }
		if (this.value instanceof Date) { return Date.class.cast(this.value); }
		return DateFormat.getDateInstance().parse(this.value.toString());
	}

	public StoredDataType getDataType() {
		return this.type;
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		StoredValue other = StoredValue.class.cast(obj);
		if (this.type != other.type) { return false; }
		if (!Tools.equals(this.value, other.value)) { return false; }
		return true;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.type, this.value);
	}
}