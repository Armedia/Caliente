package com.armedia.cmf.storage.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.armedia.cmf.storage.CmsAttribute;
import com.armedia.cmf.storage.CmsObject;
import com.armedia.cmf.storage.CmsObjectType;
import com.armedia.cmf.storage.CmsProperty;

public class JdbcCmsObjectLoader {

	static CmsObject loadObject(ResultSet rs) throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		CmsObjectType type = CmsObjectType.valueOf(rs.getString("object_type"));
		String id = rs.getString("object_id");
		String batchId = rs.getString("batch_id");
		String label = rs.getString("object_label");
		String subtype = rs.getString("object_subtype");
		return new CmsObject(type, id, batchId, label, subtype);
	}

	static CmsProperty loadProperty(ResultSet rs) throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		String name = rs.getString("name");
		String type = rs.getString("data_type");
		boolean repeating = rs.getBoolean("repeating") && !rs.wasNull();
		return new CmsProperty(name, type, repeating);
	}

	static CmsAttribute loadAttribute(ResultSet rs) throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		String name = rs.getString("name");
		String type = rs.getString("data_type");
		boolean repeating = rs.getBoolean("repeating") && !rs.wasNull();
		String id = rs.getString("id");
		boolean qualifiable = rs.getBoolean("qualifiable") && !rs.wasNull();
		int length = rs.getInt("length");
		return new CmsAttribute(name, type, id, length, repeating, qualifiable);
	}

	static void loadValues(ResultSet rs, CmsProperty property) throws SQLException {
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

	static void loadAttributes(ResultSet rs, CmsObject obj) throws SQLException {
		List<CmsAttribute> attributes = new LinkedList<CmsAttribute>();
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		while (rs.next()) {
			attributes.add(JdbcCmsObjectLoader.loadAttribute(rs));
		}
		obj.setAttributes(attributes);
	}

	static void loadProperties(ResultSet rs, CmsObject obj) throws SQLException {
		List<CmsProperty> properties = new LinkedList<CmsProperty>();
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		while (rs.next()) {
			properties.add(JdbcCmsObjectLoader.loadProperty(rs));
		}
		obj.setProperties(properties);
	}
}