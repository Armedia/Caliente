package com.delta.cmsmf.datastore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.delta.cmsmf.cmsobjects.DctmObjectType;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;

public class DataObject implements Iterable<DataAttribute> {

	private final DctmObjectType type;
	private final String id;
	private final Map<String, DataAttribute> attributes;
	private final Map<String, DataProperty> properties;

	DataObject(ResultSet rs) throws SQLException {
		this.id = rs.getString("object_id");
		this.type = DctmObjectType.valueOf(rs.getString("object_type"));
		this.attributes = new HashMap<String, DataAttribute>();
		this.properties = new HashMap<String, DataProperty>();
	}

	void loadAttributes(ResultSet rs) throws SQLException {
		boolean ok = false;
		try {
			while (rs.next()) {
				DataAttribute attribute = new DataAttribute(rs);
				this.attributes.put(attribute.getName(), attribute);
			}
			ok = true;
		} finally {
			if (!ok) {
				this.attributes.clear();
			}
		}
	}

	void loadProperties(ResultSet rs) throws SQLException {
		boolean ok = false;
		try {
			while (rs.next()) {
				DataProperty property = new DataProperty(rs);
				this.properties.put(property.getName(), property);
			}
			ok = true;
		} finally {
			if (!ok) {
				this.properties.clear();
			}
		}
	}

	public DataObject(IDfPersistentObject object) throws DfException {
		this(object, null);
	}

	public DataObject(IDfPersistentObject object, DataAttributeEncoder dataAttributeEncoder) throws DfException {
		if (dataAttributeEncoder == null) {
			dataAttributeEncoder = DataAttribute.DEFAULT_ENCODER;
		}
		this.id = object.getObjectId().getId();
		this.type = DctmObjectType.decode(object.getType().getName());
		final int attCount = object.getAttrCount();
		this.attributes = new HashMap<String, DataAttribute>(object.getAttrCount());
		for (int i = 0; i < attCount; i++) {
			DataAttribute attribute = dataAttributeEncoder.encode(object, object.getAttr(i));
			this.attributes.put(attribute.getName(), attribute);
		}
		this.properties = new HashMap<String, DataProperty>();
	}

	public DataObject(DctmObjectType type, String id, DataAttribute... attributes) {
		this(type, id, Arrays.asList(attributes));
	}

	public DataObject(DctmObjectType type, String id, Collection<DataAttribute> attributes) {
		this.type = type;
		this.id = id;
		this.attributes = new HashMap<String, DataAttribute>(attributes.size());
		for (DataAttribute attribute : attributes) {
			this.attributes.put(attribute.getName(), attribute);
		}
		this.properties = new HashMap<String, DataProperty>();
	}

	public DctmObjectType getType() {
		return this.type;
	}

	public String getId() {
		return this.id;
	}

	public DataAttribute getAttribute(String name) {
		return this.attributes.get(name);
	}

	public Set<String> getAttributeNames() {
		return Collections.unmodifiableSet(this.attributes.keySet());
	}

	public int getAttributeCount() {
		return this.attributes.size();
	}

	public DataProperty getProperty(String name) {
		return this.properties.get(name);
	}

	public Set<String> getPropertyNames() {
		return Collections.unmodifiableSet(this.properties.keySet());
	}

	public int getPropertyCount() {
		return this.properties.size();
	}

	public DataProperty removeProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Property name must not be null"); }
		return this.properties.remove(name);
	}

	public DataProperty setProperty(DataProperty property) {
		if (property == null) { throw new IllegalArgumentException("Property must not be null"); }
		return this.properties.put(property.getName(), property);
	}

	@Override
	public Iterator<DataAttribute> iterator() {
		return new Iterator<DataAttribute>() {

			private final Iterator<DataAttribute> it = DataObject.this.attributes.values().iterator();

			@Override
			public boolean hasNext() {
				return this.it.hasNext();
			}

			@Override
			public DataAttribute next() {
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
		return String.format("DataObject [type=%s, id=%s, attributes=%s, properties=%s]", this.type, this.id,
			this.attributes.values(), this.properties.values());
	}
}