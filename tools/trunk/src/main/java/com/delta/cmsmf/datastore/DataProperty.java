package com.delta.cmsmf.datastore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class DataProperty implements Iterable<IDfValue> {

	public static final DataAttributeEncoder DEFAULT_ENCODER = new DataAttributeEncoder();

	private final String name;
	private final DataType type;
	private final List<IDfValue> values;

	DataProperty(ResultSet rs) throws SQLException {
		this.name = rs.getString("name");
		this.type = DataType.valueOf(rs.getString("type"));
		this.values = new ArrayList<IDfValue>();
	}

	void loadValues(ResultSet rs) throws SQLException {
		boolean ok = false;
		try {
			while (rs.next()) {
				boolean nulled = rs.getBoolean("is_null");
				if (nulled) {
					// TODO: Should we add an IDfValue that represents null?
					this.values.add(null);
					continue;
				}
				this.values.add(this.type.decode(rs.getString("data")));
			}
			ok = true;
		} finally {
			if (!ok) {
				this.values.clear();
			}
		}
	}

	public DataProperty(IDfPersistentObject obj, IDfAttr attr) throws DfException {
		this.name = attr.getName();
		this.type = DataType.fromDfConstant(attr.getDataType());
		final int valueCount = obj.getValueCount(this.name);
		this.values = new ArrayList<IDfValue>(valueCount);
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
		final int valueCount = values.size();
		this.values = new ArrayList<IDfValue>(valueCount);
		for (IDfValue o : values) {
			this.values.add(o);
		}
	}

	public DataProperty(String name, DataType type, IDfValue... values) {
		this(name, type, Arrays.asList(values));
	}

	public DataProperty(String name, DataType type, Collection<IDfValue> values) {
		if (values == null) {
			values = Collections.emptyList();
		}
		this.name = name;
		this.type = type;
		final int valueCount = values.size();
		this.values = new ArrayList<IDfValue>(valueCount);
		for (IDfValue o : values) {
			this.values.add(o);
		}
	}

	public final DataType getType() {
		return this.type;
	}

	public final String getName() {
		return this.name;
	}

	public final int getValueCount() {
		return this.values.size();
	}

	private final void validateIndex(int idx) {
		if ((idx < 0) || (idx >= this.values.size())) { throw new ArrayIndexOutOfBoundsException(idx); }
	}

	public final boolean hasValues() {
		return !this.values.isEmpty();
	}

	public void setAllValues(Collection<IDfValue> values) {
		this.values.clear();
		if ((values != null) && !values.isEmpty()) {
			this.values.addAll(values);
		}
	}

	public final List<IDfValue> getAllValues() {
		return this.values;
	}

	public final void addValue(IDfValue value) {
		this.values.add(value);
	}

	public final IDfValue removeValue(int idx) {
		validateIndex(idx);
		return this.values.remove(idx);
	}

	public final IDfValue getValue(int idx) {
		validateIndex(idx);
		return this.values.get(idx);
	}

	public final IDfValue getSingleValue() {
		if (this.values.isEmpty()) { return null; }
		return getValue(0);
	}

	public boolean isSame(DataProperty other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!this.name.equals(other.name)) { return false; }
		if (this.type != other.type) { return false; }
		return true;
	}

	@Override
	public final Iterator<IDfValue> iterator() {
		return this.values.iterator();
	}

	@Override
	public String toString() {
		return String.format("DataProperty [name=%s, type=%s, values=%s]", this.name, this.type, this.values);
	}
}