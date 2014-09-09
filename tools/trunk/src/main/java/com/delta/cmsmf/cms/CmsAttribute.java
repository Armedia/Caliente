package com.delta.cmsmf.cms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class CmsAttribute extends CmsProperty {

	private final String id;
	private final boolean qualifiable;
	private final int length;

	CmsAttribute(ResultSet rs) throws SQLException {
		super(rs);
		this.id = rs.getString("id");
		this.qualifiable = rs.getBoolean("qualifiable");
		this.length = rs.getInt("length");
	}

	public CmsAttribute(IDfAttr attr, IDfPersistentObject obj) throws DfException {
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

	public CmsAttribute(IDfAttr attr, IDfValue... values) throws DfException {
		this(attr, Arrays.asList(values));
	}

	public CmsAttribute(IDfAttr attr, Collection<IDfValue> values) throws DfException {
		super(attr, values);
		this.id = attr.getId();
		this.length = attr.getLength();
		this.qualifiable = attr.isQualifiable();
	}

	public CmsAttribute(String name, CmsDataType type, String id, int length, boolean repeating, boolean qualifiable,
		IDfValue... values) {
		this(name, type, id, length, repeating, qualifiable, Arrays.asList(values));
	}

	public CmsAttribute(String name, CmsDataType type, String id, int length, boolean repeating, boolean qualifiable,
		Collection<IDfValue> values) {
		super(name, type, repeating, values);
		if (id == null) { throw new IllegalArgumentException("Must provide a non-null attribute id"); }
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

	public final boolean isSame(CmsAttribute other) {
		if (!super.isSame(other)) { return false; }
		if (!Tools.equals(this.id, other.id)) { return false; }
		if (this.qualifiable != other.qualifiable) { return false; }
		if (this.length != other.length) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("CmsAttribute [name=%s, type=%s, id=%s, repeating=%s, qualifiable=%s, length=%d %s=%s]",
			getName(), getType(), this.id, isRepeating(), this.qualifiable, this.length, (isRepeating() ? "values"
				: "singleValue"), (isRepeating() ? getValues() : getValue()));
	}
}