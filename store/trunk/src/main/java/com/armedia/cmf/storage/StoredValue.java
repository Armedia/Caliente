package com.armedia.cmf.storage;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import com.armedia.commons.utilities.Tools;

public final class StoredValue {

	private final StoredDataType type;
	private final Object value;
	private final boolean nullValue;

	public StoredValue(int value) {
		this.type = StoredDataType.INTEGER;
		this.value = value;
		this.nullValue = false;
	}

	public StoredValue(boolean value) {
		this.type = StoredDataType.BOOLEAN;
		this.value = value;
		this.nullValue = false;
	}

	public StoredValue(double value) {
		this.type = StoredDataType.DOUBLE;
		this.value = value;
		this.nullValue = false;
	}

	public StoredValue(String value) {
		this.type = StoredDataType.STRING;
		this.value = value;
		this.nullValue = (value == null);
	}

	public StoredValue(Date value) {
		this.type = StoredDataType.TIME;
		this.value = value;
		this.nullValue = (value == null);
	}

	public StoredValue(Calendar value) {
		this.type = StoredDataType.TIME;
		this.value = (value != null ? value.getTime() : null);
		this.nullValue = (value == null);
	}

	public StoredValue(StoredDataType type, Object value) throws ParseException {
		this.type = type;
		this.nullValue = (value == null);
		if (value != null) {
			switch (type) {
				case INTEGER:
					if (value instanceof Number) {
						this.value = Number.class.cast(value);
					} else {
						this.value = Integer.valueOf(value.toString());
					}
					break;
				case BOOLEAN:
					if (value instanceof Boolean) {
						this.value = Boolean.class.cast(value);
					} else {
						this.value = Boolean.valueOf(value.toString());
					}
					break;
				case DOUBLE:
					if (value instanceof Number) {
						this.value = Number.class.cast(value);
					} else {
						this.value = Double.valueOf(value.toString());
					}
					break;
				case STRING:
				case ID:
					this.value = Tools.toString(value);
					break;
				case TIME:
					if (value instanceof Date) {
						this.value = Date.class.cast(value);
					} else if (value instanceof Calendar) {
						this.value = Calendar.class.cast(value).getTime();
					} else {
						this.value = DateFormat.getDateInstance().parse(value.toString());
					}
					break;
				default:
					throw new IllegalArgumentException(String.format("Unsupported data type [%s]", type));
			}
		} else {
			this.value = value;
		}
	}

	public String asString() {
		return Tools.toString(this.value);
	}

	public String asId() {
		return asString();
	}

	public int asInteger() {
		if (this.nullValue) { return 0; }
		if (this.value instanceof Number) { return Number.class.cast(this.value).intValue(); }
		return Integer.valueOf(this.value.toString());
	}

	public boolean asBoolean() {
		if (this.nullValue) { return false; }
		if (this.value instanceof Boolean) { return Boolean.class.cast(this.value).booleanValue(); }
		return Boolean.valueOf(this.value.toString());
	}

	public double asDouble() {
		if (this.nullValue) { return Double.NaN; }
		if (this.value instanceof Number) { return Number.class.cast(this.value).doubleValue(); }
		return Double.valueOf(this.value.toString());
	}

	public Date asTime() throws ParseException {
		if (this.nullValue) { return null; }
		if (this.value instanceof Date) { return Date.class.cast(this.value); }
		return DateFormat.getDateInstance().parse(this.value.toString());
	}

	public StoredDataType getDataType() {
		return this.type;
	}

	public boolean isNull() {
		return this.nullValue;
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

	@Override
	public String toString() {
		return asString();
	}
}