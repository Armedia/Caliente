package com.armedia.cmf.storage.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;

public class JdbcStoredObjectLoader {

	static StoredObject loadObject(ResultSet rs) throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		StoredObjectType type = StoredObjectType.valueOf(rs.getString("object_type"));
		String id = rs.getString("object_id");
		String batchId = rs.getString("batch_id");
		String label = rs.getString("object_label");
		String subtype = rs.getString("object_subtype");
		return new StoredObject(type, id, batchId, label, subtype);
	}

	static StoredProperty loadProperty(ResultSet rs) throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		String name = rs.getString("name");
		String type = rs.getString("data_type");
		boolean repeating = rs.getBoolean("repeating") && !rs.wasNull();
		return new StoredProperty(name, type, repeating);
	}

	static StoredAttribute loadAttribute(ResultSet rs) throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		String name = rs.getString("name");
		String type = rs.getString("data_type");
		boolean repeating = rs.getBoolean("repeating") && !rs.wasNull();
		String id = rs.getString("id");
		boolean qualifiable = rs.getBoolean("qualifiable") && !rs.wasNull();
		int length = rs.getInt("length");
		return new StoredAttribute(name, type, id, length, repeating, qualifiable);
	}

	static void loadValues(ResultSet rs, StoredProperty property) throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		List<String> values = new LinkedList<String>();
		while (rs.next()) {
			values.add(rs.getString("data"));
			if (!property.isRepeating()) {
				break;
			}
		}
		property.setValues(values);
	}

	static void loadAttributes(ResultSet rs, StoredObject obj) throws SQLException {
		List<StoredAttribute> attributes = new LinkedList<StoredAttribute>();
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		while (rs.next()) {
			attributes.add(JdbcStoredObjectLoader.loadAttribute(rs));
		}
		obj.setAttributes(attributes);
	}

	static void loadProperties(ResultSet rs, StoredObject obj) throws SQLException {
		List<StoredProperty> properties = new LinkedList<StoredProperty>();
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		while (rs.next()) {
			properties.add(JdbcStoredObjectLoader.loadProperty(rs));
		}
		obj.setProperties(properties);
	}
}