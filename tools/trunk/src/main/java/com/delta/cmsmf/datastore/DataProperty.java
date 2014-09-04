package com.delta.cmsmf.datastore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.mainEngine.RepositoryConfiguration;
import com.delta.cmsmf.runtime.RunTimeProperties;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfValue;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class DataProperty implements Iterable<IDfValue> {

	public static final DataAttributeEncoder DEFAULT_ENCODER = new DataAttributeEncoder();

	private static final IDfValue DM_DBO = new DfValue(CMSMFAppConstants.DM_DBO);

	private final String name;
	private final DataType type;
	private final boolean repeating;
	private final List<IDfValue> values;
	private boolean valuesLoaded = false;

	DataProperty(ResultSet rs) throws SQLException {
		this.name = rs.getString("property_name");
		this.type = DataType.valueOf(rs.getString("property_type"));
		this.repeating = rs.getBoolean("is_repeating");
		this.values = new ArrayList<IDfValue>();
	}

	void loadValues(ResultSet rs) throws SQLException {
		if (this.valuesLoaded) { throw new IllegalArgumentException(String.format(
			"The values for property [%s] have already been loaded", this.name)); }
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

	public DataProperty(IDfPersistentObject obj, IDfAttr attr) throws DfException {
		this.name = attr.getName();
		this.type = DataType.fromDfConstant(attr.getDataType());
		this.repeating = attr.isRepeating();
		final int valueCount = obj.getValueCount(this.name);
		this.values = new ArrayList<IDfValue>(valueCount);

		final boolean checkForDbo;
		final String operatorName;
		if (this.type == DataType.DF_STRING) {
			// This only applies for string values
			checkForDbo = RunTimeProperties.getRunTimePropertiesInstance().getAttrsToCheckForRepoOperatorName()
				.contains(attr.getName());
			operatorName = RepositoryConfiguration.getRepositoryConfiguration().getOperatorName();
		} else {
			checkForDbo = false;
			operatorName = null;
		}
		for (int i = 0; i < valueCount; i++) {
			IDfValue value = obj.getRepeatingValue(this.name, i);
			// If this is the operator name, we replace the value
			if (checkForDbo && (value != null) && operatorName.equals(value.asString())) {
				value = DataProperty.DM_DBO;
			}
			this.values.add(value);
		}
		this.valuesLoaded = true;
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
		final int valueCount = (this.repeating ? values.size() : 1);
		this.values = new ArrayList<IDfValue>(valueCount);
		for (IDfValue o : values) {
			this.values.add(o);
			// If not repeating, only add the first value
			if (this.repeating) {
				break;
			}
		}
		this.valuesLoaded = true;
	}

	public DataProperty(String name, DataType type, boolean repeating, IDfValue... values) {
		this(name, type, repeating, Arrays.asList(values));
	}

	public DataProperty(String name, DataType type, boolean repeating, Collection<IDfValue> values) {
		if (values == null) {
			values = Collections.emptyList();
		}
		this.name = name;
		this.type = type;
		this.repeating = repeating;
		final int valueCount = values.size();
		this.values = new ArrayList<IDfValue>(valueCount);
		for (IDfValue o : values) {
			this.values.add(o);
			// If not repeating, only add the first value
			if (this.repeating) {
				break;
			}
		}
		this.valuesLoaded = true;
	}

	public DataType getType() {
		return this.type;
	}

	public boolean isRepeating() {
		return this.repeating;
	}

	public String getName() {
		return this.name;
	}

	public int getValueCount() {
		return this.values.size();
	}

	private void validateIndex(int idx) {
		if ((idx < 0) || (idx >= this.values.size())) { throw new ArrayIndexOutOfBoundsException(idx); }
	}

	public boolean hasValues() {
		return !this.values.isEmpty();
	}

	public void setAllValues(Collection<IDfValue> values) {
		this.values.clear();
		if ((values != null) && !values.isEmpty()) {
			this.values.addAll(values);
		}
	}

	public Collection<IDfValue> getAllValues() {
		return Collections.unmodifiableList(this.values);
	}

	public void addValue(IDfValue value) {
		this.values.add(value);
	}

	public IDfValue removeValue(int idx) {
		validateIndex(idx);
		return this.values.remove(idx);
	}

	public IDfValue getValue(int idx) {
		validateIndex(idx);
		return this.values.get(idx);
	}

	public IDfValue getSingleValue() {
		if (this.values.isEmpty()) { return null; }
		return getValue(0);
	}

	public boolean isSame(DataProperty other) {
		if (other == null) { return false; }
		if (!this.name.equals(other.name)) { return false; }
		if (this.type != other.type) { return false; }
		if (this.repeating != other.repeating) { return false; }
		return true;
	}

	@Override
	public Iterator<IDfValue> iterator() {
		return new Iterator<IDfValue>() {
			private final Iterator<IDfValue> it = DataProperty.this.values.iterator();

			@Override
			public boolean hasNext() {
				return this.it.hasNext();
			}

			@Override
			public IDfValue next() {
				return this.it.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public String toString() {
		return String.format("DataProperty [name=%s, type=%s, repeating=%s, valuesLoaded=%s, values=%s]", this.name,
			this.type, this.repeating, this.valuesLoaded, this.values);
	}
}