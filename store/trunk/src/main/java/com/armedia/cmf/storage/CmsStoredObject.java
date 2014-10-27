/**
 *
 */

package com.armedia.cmf.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CmsStoredObject {

	public static final String NULL_BATCH_ID = "[NO BATCHING]";

	private final CmsStoredObjectType type;

	private final String id;
	private final String batchId;
	private final String label;
	private final String subtype;
	private final Map<String, CmsStoredAttribute> attributes = new HashMap<String, CmsStoredAttribute>();
	private final Map<String, CmsStoredProperty> properties = new HashMap<String, CmsStoredProperty>();

	public CmsStoredObject(CmsStoredObjectType type, String id, String batchId, String label, String subtype) {
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

	public final CmsStoredObjectType getType() {
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

	public final CmsStoredAttribute getAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to retrieve"); }
		return this.attributes.get(name);
	}

	public final CmsStoredAttribute setAttribute(CmsStoredAttribute attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to set"); }
		return this.attributes.put(attribute.getName(), attribute);
	}

	public final CmsStoredAttribute removeAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to remove"); }
		return this.attributes.remove(name);
	}

	public final Collection<CmsStoredAttribute> getAttributes() {
		return Collections.unmodifiableCollection(this.attributes.values());
	}

	public final void setAttributes(Collection<CmsStoredAttribute> attributes) {
		this.attributes.clear();
		for (CmsStoredAttribute att : attributes) {
			setAttribute(att);
		}
	}

	public final int getPropertyCount() {
		return this.properties.size();
	}

	public final Set<String> getPropertyNames() {
		return Collections.unmodifiableSet(this.properties.keySet());
	}

	public final CmsStoredProperty getProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to retrieve"); }
		return this.properties.get(name);
	}

	public final CmsStoredProperty setProperty(CmsStoredProperty property) {
		if (property == null) { throw new IllegalArgumentException("Must provide a property to set"); }
		return this.properties.put(property.getName(), property);
	}

	public final CmsStoredProperty removeProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to remove"); }
		return this.properties.remove(name);
	}

	public final Collection<CmsStoredProperty> getProperties() {
		return Collections.unmodifiableCollection(this.properties.values());
	}

	public final void setProperties(Collection<CmsStoredProperty> properties) {
		this.attributes.clear();
		for (CmsStoredProperty prop : properties) {
			setProperty(prop);
		}
	}

	protected String toStringTrailer() {
		return "";
	}

	@Override
	public final String toString() {
		final String trailer = toStringTrailer();
		final String trailerSep = ((trailer != null) && (trailer.length() > 0) ? ", " : "");
		return String.format("%s [type=%s, subtype=%s, id=%s, batchId=%s, label=%s%s%s]", getClass().getSimpleName(),
			this.type, this.subtype, this.id, this.batchId, this.label, trailerSep, trailer);
	}
}