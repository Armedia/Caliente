package com.delta.cmsmf.datastore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class DataAttribute extends DataProperty {

	public static final DataAttributeEncoder DEFAULT_ENCODER = new DataAttributeEncoder();

	private final String id;
	private final boolean repeating;
	private final boolean qualifiable;
	private final int length;

	DataAttribute(ResultSet rs) throws SQLException {
		super(rs);
		this.id = rs.getString("id");
		this.repeating = rs.getBoolean("is_repeating");
		this.qualifiable = rs.getBoolean("is_qualifiable");
		this.length = rs.getInt("length");
	}

	public DataAttribute(IDfPersistentObject obj, IDfAttr attr) throws DfException {
		super(obj, attr);
		this.id = attr.getId();
		this.length = attr.getLength();
		this.repeating = attr.isRepeating();
		this.qualifiable = attr.isQualifiable();
		setAllValues(DfValueFactory.getAllRepeatingValues(obj, attr));
	}

	public DataAttribute(IDfPersistentObject obj, IDfAttr attr, IDfValue... values) throws DfException {
		this(obj, attr, Arrays.asList(values));
	}

	public DataAttribute(IDfPersistentObject obj, IDfAttr attr, Collection<IDfValue> values) throws DfException {
		super(obj, attr, values);
		this.id = attr.getId();
		this.length = attr.getLength();
		this.repeating = attr.isRepeating();
		this.qualifiable = attr.isQualifiable();
		if (!this.repeating) {
			if (values == null) {
				values = Collections.emptyList();
			}
			// TODO: Use a null value? Really?
			IDfValue newValue = null;
			if (!values.isEmpty()) {
				newValue = values.iterator().next();
			}
			addValue(newValue);
		}
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
		this.repeating = repeating;
		this.qualifiable = qualifiable;
		if (!this.repeating) {
			if (values == null) {
				values = Collections.emptyList();
			}
			// TODO: Use a null value? Really?
			IDfValue newValue = null;
			if (!values.isEmpty()) {
				newValue = values.iterator().next();
			}
			addValue(newValue);
		}
	}

	public final boolean isRepeating() {
		return this.repeating;
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
		if (this.repeating != other.repeating) { return false; }
		if (this.qualifiable != other.qualifiable) { return false; }
		if (this.length != other.length) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format(
			"DataAttribute [name=%s, type=%s, id=%s, repeating=%s, qualifiable=%s, length=%s, values=%s]", getName(),
			getType(), this.id, this.repeating, this.qualifiable, this.length, getAllValues());
	}
}