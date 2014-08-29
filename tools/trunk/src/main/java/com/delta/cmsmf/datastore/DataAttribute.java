package com.delta.cmsmf.datastore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class DataAttribute implements Iterable<IDfValue> {

	private final String name;
	private final String id;
	private final DataType type;
	private final boolean repeating;
	private final boolean qualifiable;
	private final int length;
	private final ArrayList<IDfValue> values;
	private boolean valuesLoaded = false;

	DataAttribute(ResultSet rs) throws SQLException {
		this.name = rs.getString("attribute_name");
		this.id = rs.getString("attribute_id");
		this.type = DataType.values()[rs.getInt("attribute_type")];
		this.repeating = rs.getBoolean("is_repeating");
		this.qualifiable = rs.getBoolean("is_qualifiable");
		this.length = rs.getInt("attribute_length");
		this.values = new ArrayList<IDfValue>();
	}

	void loadValues(ResultSet rs) throws SQLException {
		if (this.valuesLoaded) { throw new IllegalArgumentException(String.format(
			"The values for attribute [%s] have already been loaded", this.name)); }
		boolean ok = false;
		try {
			while (rs.next()) {
				boolean nulled = rs.getBoolean("is_null");
				if (nulled) {
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
			this.valuesLoaded = ok;
		}
	}

	public DataAttribute(IDfPersistentObject obj, IDfAttr attr) throws DfException {
		this.name = attr.getName();
		this.id = attr.getId();
		this.type = DataType.fromDfConstant(attr.getDataType());
		this.length = attr.getLength();
		this.repeating = attr.isRepeating();
		this.qualifiable = attr.isQualifiable();
		final int valueCount = obj.getValueCount(this.name);
		this.values = new ArrayList<IDfValue>(valueCount);
		for (int i = 0; i < valueCount; i++) {
			this.values.add(obj.getRepeatingValue(this.name, i));
		}
		this.valuesLoaded = true;
	}

	public DataAttribute(String name, String id, DataType type, int length, boolean repeating, boolean qualifiable,
		IDfValue... values) {
		this(name, id, type, length, repeating, qualifiable, Arrays.asList(values));
	}

	public DataAttribute(String name, String id, DataType type, int length, boolean repeating, boolean qualifiable,
		Collection<IDfValue> values) {
		this.name = name;
		this.id = id;
		this.type = type;
		this.length = length;
		this.repeating = repeating;
		this.qualifiable = qualifiable;
		final int valueCount = values.size();
		this.values = new ArrayList<IDfValue>(valueCount);
		for (IDfValue o : values) {
			this.values.add(o);
		}
		this.valuesLoaded = true;
	}

	public DataType getType() {
		return this.type;
	}

	public boolean isRepeating() {
		return this.repeating;
	}

	public boolean isQualifiable() {
		return this.qualifiable;
	}

	public String getName() {
		return this.name;
	}

	public String getId() {
		return this.id;
	}

	public int getLength() {
		return this.length;
	}

	public int getValueCount() {
		return this.values.size();
	}

	public IDfValue getValue(int idx) {
		if ((idx < 0) || (idx >= this.values.size())) { throw new ArrayIndexOutOfBoundsException(idx); }
		return this.values.get(idx);
	}

	@Override
	public Iterator<IDfValue> iterator() {
		return this.values.iterator();
	}
}