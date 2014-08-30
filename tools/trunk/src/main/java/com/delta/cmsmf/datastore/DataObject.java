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
	private final boolean contentHolder;
	private final String contentPath;
	private final Map<String, DataAttribute> attributes;
	private boolean attributesLoaded = false;

	DataObject(ResultSet rs) throws SQLException {
		this.id = rs.getString("object_id");
		this.type = DctmObjectType.valueOf(rs.getString("object_type"));
		this.contentHolder = rs.getBoolean("has_content");
		this.contentPath = rs.getString("content_path");
		this.attributes = new HashMap<String, DataAttribute>();
	}

	void loadAttributes(ResultSet rs) throws SQLException {
		if (this.attributesLoaded) { throw new IllegalArgumentException(String.format(
			"The attributes for object [%s] have already been loaded", this.id)); }
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
			this.attributesLoaded = ok;
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
		this.contentHolder = false; // TODO: how to tell?
		this.contentPath = null; // TODO: how to calculate?
		final int attCount = object.getAttrCount();
		this.attributes = new HashMap<String, DataAttribute>(object.getAttrCount());
		for (int i = 0; i < attCount; i++) {
			DataAttribute attribute = dataAttributeEncoder.encode(object, object.getAttr(i));
			this.attributes.put(attribute.getName(), attribute);
		}
		this.attributesLoaded = true;
	}

	public DataObject(DctmObjectType type, String id, boolean contentHolder, String contentPath,
		DataAttribute... attributes) {
		this(type, id, contentHolder, contentPath, Arrays.asList(attributes));
	}

	public DataObject(DctmObjectType type, String id, boolean contentHolder, String contentPath,
		Collection<DataAttribute> attributes) {
		this.type = type;
		this.id = id;
		this.contentHolder = contentHolder;
		this.contentPath = contentPath;
		this.attributes = new HashMap<String, DataAttribute>(attributes.size());
		for (DataAttribute attribute : attributes) {
			this.attributes.put(attribute.getName(), attribute);
		}
		this.attributesLoaded = true;
	}

	public DctmObjectType getType() {
		return this.type;
	}

	public String getId() {
		return this.id;
	}

	public boolean isContentHolder() {
		return this.contentHolder;
	}

	public String getContentPath() {
		return this.contentPath;
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
		return String.format(
			"DataObject [type=%s, id=%s, contentHolder=%s, contentPath=%s, attributesLoaded=%s, attributes=%s]",
			this.type, this.id, this.contentHolder, this.contentPath, this.attributesLoaded, this.attributes.values());
	}
}