/**
 *
 */

package com.delta.cmsmf.cms.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.delta.cmsmf.exception.CMSMFException;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public abstract class CmsObject {

	public static final String NULL_BATCH_ID = "[NO BATCHING]";

	protected final Logger log = Logger.getLogger(getClass());

	private final CmsObjectType type;

	private final String id;
	private final String batchId;
	private final String label;
	private final String subtype;
	private final Map<String, CmsAttribute> attributes = new HashMap<String, CmsAttribute>();
	private final Map<String, CmsProperty> properties = new HashMap<String, CmsProperty>();

	public CmsObject(CmsObjectType type, String id, String batchId, String label, String subtype) {
		if (type == null) { throw new IllegalArgumentException("Must provide a valid object type"); }
		if (id == null) { throw new IllegalArgumentException("Must provide a valid object id"); }
		if (label == null) { throw new IllegalArgumentException("Must provide a valid object label"); }
		if (subtype == null) { throw new IllegalArgumentException("Must provide a valid object subtype"); }
		this.type = type;
		this.id = id;
		this.batchId = batchId;
		this.label = label;
		this.subtype = subtype;
	}

	public CmsObject(ResultSet rs) throws SQLException, UnsupportedObjectTypeException {
		this.type = CmsObjectType.decode(rs.getString("object_type"));
		this.id = rs.getString("object_id");
		this.batchId = rs.getString("batch_id");
		this.label = rs.getString("object_label");
		this.subtype = rs.getString("object_subtype");
		this.attributes.clear();
		this.properties.clear();
	}

	public final void loadAttributes(ResultSet rs) throws SQLException {
		boolean ok = false;
		try {
			this.attributes.clear();
			while (rs.next()) {
				CmsAttribute attribute = new CmsAttribute(rs);
				this.attributes.put(attribute.getName(), attribute);
			}
			ok = true;
		} finally {
			if (!ok) {
				this.attributes.clear();
			}
		}
	}

	public final void loadProperties(ResultSet rs) throws SQLException {
		boolean ok = false;
		try {
			this.properties.clear();
			while (rs.next()) {
				CmsProperty property = new CmsProperty(rs);
				this.properties.put(property.getName(), property);
			}
			ok = true;
		} finally {
			if (!ok) {
				this.properties.clear();
			}
		}
	}

	public void loadCompleted() throws CMSMFException {
	}

	public final CmsObjectType getType() {
		return this.type;
	}

	public final String getSubtype() {
		return this.subtype;
	}

	public final String getId() {
		return this.id;
	}

	public final String getBatchId() {
		return this.batchId;
	}

	public final String getLabel() {
		return this.label;
	}

	public final int getAttributeCount() {
		return this.attributes.size();
	}

	public final Set<String> getAttributeNames() {
		return Collections.unmodifiableSet(this.attributes.keySet());
	}

	public final CmsAttribute getAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to retrieve"); }
		return this.attributes.get(name);
	}

	public final CmsAttribute setAttribute(CmsAttribute attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to set"); }
		return this.attributes.put(attribute.getName(), attribute);
	}

	public final CmsAttribute removeAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to remove"); }
		return this.attributes.remove(name);
	}

	public final Collection<CmsAttribute> getAllAttributes() {
		return Collections.unmodifiableCollection(this.attributes.values());
	}

	public final int getPropertyCount() {
		return this.properties.size();
	}

	public final Set<String> getPropertyNames() {
		return Collections.unmodifiableSet(this.properties.keySet());
	}

	public final CmsProperty getProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to retrieve"); }
		return this.properties.get(name);
	}

	public final CmsProperty setProperty(CmsProperty property) {
		if (property == null) { throw new IllegalArgumentException("Must provide a property to set"); }
		return this.properties.put(property.getName(), property);
	}

	public final CmsProperty removeProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to remove"); }
		return this.properties.remove(name);
	}

	public final Collection<CmsProperty> getAllProperties() {
		return Collections.unmodifiableCollection(this.properties.values());
	}

	@Override
	public String toString() {
		return String.format("CmsObject [type=%s, subtype=%s, id=%s, label=%s]", this.type, this.subtype, this.id,
			this.label);
	}
}