package com.delta.cmsmf.datastore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class DataProperty implements Iterable<IDfValue> {

	public static final DataAttributeEncoder DEFAULT_ENCODER = new DataAttributeEncoder();

	private final String name;
	private final DataType type;
	private final boolean repeating;

	private IDfValue singleValue = null;
	private final List<IDfValue> values;

	DataProperty(ResultSet rs) throws SQLException {
		this.name = rs.getString("name");
		this.type = DataType.valueOf(rs.getString("type"));
		this.repeating = rs.getBoolean("is_repeating");
		this.values = new ArrayList<IDfValue>();
	}

	void loadValues(ResultSet rs) throws SQLException {
		boolean ok = false;
		try {
			while (rs.next()) {
				IDfValue next = this.type.decode(rs.getString("data"));
				if (this.repeating) {
					this.values.add(next);
				} else {
					this.singleValue = next;
					break;
				}
			}
			ok = true;
		} finally {
			if (!ok) {
				this.values.clear();
				this.singleValue = null;
			}
		}
	}

	public DataProperty(IDfPersistentObject obj, IDfAttr attr) throws DfException {
		this.name = attr.getName();
		this.type = DataType.fromDfConstant(attr.getDataType());
		final int valueCount = obj.getValueCount(this.name);
		this.repeating = attr.isRepeating();
		this.values = new ArrayList<IDfValue>(valueCount);
		if (this.repeating) {
			for (int i = 0; i < valueCount; i++) {
				this.values.add(obj.getRepeatingValue(this.name, i));
			}
			this.singleValue = null;
		} else {
			this.singleValue = obj.getValue(this.name);
		}
	}

	public DataProperty(IDfPersistentObject obj, IDfAttr attr, IDfValue... values) throws DfException {
		this(obj, attr, Arrays.asList(values));
	}

	public DataProperty(IDfPersistentObject obj, IDfAttr attr, Collection<IDfValue> values) throws DfException {
		if (values == null) {
			values = Collections.emptyList();
		}
		this.name = attr.getName();
		this.type = DataType.fromDfConstant(attr.getDataType());
		this.repeating = attr.isRepeating();
		this.values = new ArrayList<IDfValue>(values.size());
		setValues(values);
	}

	public DataProperty(String name, DataType type, IDfValue... values) {
		this(name, type, true, Arrays.asList(values));
	}

	public DataProperty(String name, DataType type, boolean repeating, IDfValue... values) {
		this(name, type, repeating, Arrays.asList(values));
	}

	public DataProperty(String name, DataType type, Collection<IDfValue> values) {
		this(name, type, true, values);
	}

	public DataProperty(String name, DataType type, boolean repeating, Collection<IDfValue> values) {
		if (values == null) {
			values = Collections.emptyList();
		}
		this.name = name;
		this.type = type;
		final int valueCount = values.size();
		this.repeating = repeating;
		this.values = new ArrayList<IDfValue>(valueCount);
		setValues(values);
	}

	public final String getName() {
		return this.name;
	}

	public final DataType getType() {
		return this.type;
	}

	public final boolean isRepeating() {
		return this.repeating;
	}

	public final int getValueCount() {
		if (!this.repeating) { return 1; }
		return this.values.size();
	}

	private int sanitizeIndex(int idx) {
		if (idx < 0) {
			idx = 0;
		}
		if ((!this.repeating && (idx > 0)) || (idx >= this.values.size())) { throw new ArrayIndexOutOfBoundsException(
			idx); }
		return idx;
	}

	public final boolean hasValues() {
		return !this.values.isEmpty();
	}

	public final void setValues(IDfValue... values) {
		setValues(Arrays.asList(values));
	}

	public final void setValues(Collection<IDfValue> values) {
		this.values.clear();
		if (values == null) {
			values = Collections.emptyList();
		}
		if (this.repeating) {
			for (IDfValue o : values) {
				this.values.add(o != null ? o : this.type.getNullValue());
			}
		} else {
			IDfValue value = null;
			if (!values.isEmpty()) {
				value = values.iterator().next();
			}
			this.singleValue = Tools.coalesce(value, this.type.getNullValue());
		}
	}

	public final List<IDfValue> getValues() {
		if (this.repeating) { return this.values; }
		return Collections.singletonList(this.singleValue);
	}

	public final void addValue(IDfValue value) {
		if (this.repeating) {
			this.values.add(value);
		} else {
			throw new UnsupportedOperationException("This is a single-valued property, cannot add another value");
		}
	}

	public final void setValue(IDfValue value) {
		if (value == null) {
			value = this.type.getNullValue();
		}
		if (this.repeating) {
			this.values.clear();
			this.values.add(value);
		} else {
			this.singleValue = value;
		}
	}

	public final void clearValue() {
		setValues((Collection<IDfValue>) null);
	}

	public final IDfValue removeValue(int idx) {
		idx = sanitizeIndex(idx);
		if (this.repeating) { return this.values.remove(idx); }
		IDfValue old = this.singleValue;
		this.singleValue = this.type.getNullValue();
		return old;
	}

	public final IDfValue getValue(int idx) {
		idx = sanitizeIndex(idx);
		if (this.repeating) { return this.values.get(idx); }
		return this.singleValue;
	}

	public final IDfValue getValue() {
		return getValue(0);
	}

	public boolean isSame(DataProperty other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!this.name.equals(other.name)) { return false; }
		if (this.type != other.type) { return false; }
		if (this.repeating != other.repeating) { return false; }
		return true;
	}

	@Override
	public final Iterator<IDfValue> iterator() {
		return this.values.iterator();
	}

	@Override
	public String toString() {
		return String.format("DataProperty [name=%s, type=%s, repeating=%s, %s=%s]", this.name, this.type,
			this.repeating, (this.repeating ? "values" : "singleValue"), (this.repeating ? this.values
				: this.singleValue));
	}
}