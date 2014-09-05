package com.delta.cmsmf.datastore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class DataAttribute extends DataProperty {

	private final String id;
	private final boolean qualifiable;
	private final int length;

	DataAttribute(ResultSet rs) throws SQLException {
		super(rs);
		this.id = rs.getString("id");
		this.qualifiable = rs.getBoolean("is_qualifiable");
		this.length = rs.getInt("length");
	}

	public DataAttribute(IDfAttr attr, IDfPersistentObject obj) throws DfException {
		super(attr, obj);
		this.id = attr.getId();
		this.length = attr.getLength();
		this.qualifiable = attr.isQualifiable();
		if (attr.isRepeating()) {
			setValues(DfValueFactory.getAllRepeatingValues(attr, obj));
		} else {
			setValue(obj.getValue(attr.getName()));
		}
	}

	public DataAttribute(IDfAttr attr, IDfValue... values) throws DfException {
		this(attr, Arrays.asList(values));
	}

	public DataAttribute(IDfAttr attr, Collection<IDfValue> values) throws DfException {
		super(attr, values);
		this.id = attr.getId();
		this.length = attr.getLength();
		this.qualifiable = attr.isQualifiable();
	}

	public DataAttribute(String name, String id, DataType type, int length, boolean repeating, boolean qualifiable,
		IDfValue... values) {
		this(name, type, id, length, repeating, qualifiable, Arrays.asList(values));
	}

	public DataAttribute(String name, DataType type, String id, int length, boolean repeating, boolean qualifiable,
		Collection<IDfValue> values) {
		super(name, type, values);
		this.id = id;
		this.length = length;
		this.qualifiable = qualifiable;
	}

	public final boolean isQualifiable() {
		return this.qualifiable;
	}

	public final String getId() {
		return this.id;
	}

	public final int getLength() {
		return this.length;
	}

	public final boolean isSame(DataAttribute other) {
		if (!super.isSame(other)) { return false; }
		if (this.qualifiable != other.qualifiable) { return false; }
		if (this.length != other.length) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("DataAttribute [name=%s, type=%s, repeating=%s, qualifiable=%s, length=%d %s=%s]",
			getName(), getType(), isRepeating(), this.qualifiable, this.length, (isRepeating() ? "values"
				: "singleValue"), (isRepeating() ? getValues() : getValue()));
	}
}