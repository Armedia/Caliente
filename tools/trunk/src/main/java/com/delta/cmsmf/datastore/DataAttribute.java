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

public class DataAttribute implements Iterable<IDfValue> {

	public static final DataAttributeEncoder DEFAULT_ENCODER = new DataAttributeEncoder();

	private static final IDfValue DM_DBO = new DfValue(CMSMFAppConstants.DM_DBO);

	private final String name;
	private final String id;
	private final DataType type;
	private final boolean repeating;
	private final boolean qualifiable;
	private final int length;
	private final List<IDfValue> values;
	private boolean valuesLoaded = false;

	DataAttribute(ResultSet rs) throws SQLException {
		this.name = rs.getString("attribute_name");
		this.id = rs.getString("attribute_id");
		this.type = DataType.valueOf(rs.getString("attribute_type"));
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
				value = DataAttribute.DM_DBO;
			}
			this.values.add(value);
		}
		this.valuesLoaded = true;
	}

	public DataAttribute(IDfPersistentObject obj, IDfAttr attr, IDfValue... values) throws DfException {
		this(obj, attr, Arrays.asList(values));
	}

	public DataAttribute(IDfPersistentObject obj, IDfAttr attr, Collection<IDfValue> values) throws DfException {
		if (values == null) {
			values = Collections.emptyList();
		}
		this.name = attr.getName();
		this.id = attr.getId();
		this.type = DataType.fromDfConstant(attr.getDataType());
		this.length = attr.getLength();
		this.repeating = attr.isRepeating();
		this.qualifiable = attr.isQualifiable();
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

	public DataAttribute(String name, String id, DataType type, int length, boolean repeating, boolean qualifiable,
		IDfValue... values) {
		this(name, id, type, length, repeating, qualifiable, Arrays.asList(values));
	}

	public DataAttribute(String name, String id, DataType type, int length, boolean repeating, boolean qualifiable,
		Collection<IDfValue> values) {
		if (values == null) {
			values = Collections.emptyList();
		}
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

	public IDfValue getValue() {
		if (this.values.isEmpty()) { return null; }
		return getValue(0);
	}

	@Override
	public Iterator<IDfValue> iterator() {
		return new Iterator<IDfValue>() {
			private final Iterator<IDfValue> it = DataAttribute.this.values.iterator();

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
		return String
			.format(
				"DataAttribute [name=%s, id=%s, type=%s, repeating=%s, qualifiable=%s, length=%s, valuesLoaded=%s, values=%s]",
				this.name, this.id, this.type, this.repeating, this.qualifiable, this.length, this.valuesLoaded,
				this.values);
	}
}