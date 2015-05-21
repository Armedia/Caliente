/**
 *
 */

package com.armedia.cmf.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.armedia.commons.utilities.Tools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CmfObject<V> {

	private final CmfType type;

	private final String id;
	private final String searchKey;
	private final String batchId;
	private final String label;
	private final String subtype;
	private String relativeStreamLocation = null;
	private final Map<String, CmfAttribute<V>> attributes = new HashMap<String, CmfAttribute<V>>();
	private final Map<String, CmfProperty<V>> properties = new HashMap<String, CmfProperty<V>>();
	private CmfACL<V> acl = null;

	public CmfObject(CmfObject<V> pattern) {
		this.type = pattern.getType();
		this.id = pattern.getId();
		this.searchKey = pattern.getSearchKey();
		this.batchId = pattern.getBatchId();
		this.label = pattern.getLabel();
		this.subtype = pattern.getSubtype();
		for (CmfAttribute<V> attribute : pattern.getAttributes()) {
			this.attributes.put(attribute.getName(), new CmfAttribute<V>(attribute));
		}
		for (CmfProperty<V> property : pattern.getProperties()) {
			this.properties.put(property.getName(), new CmfProperty<V>(property));
		}
	}

	public CmfObject(CmfType type, String id, String batchId, String label, String subtype) {
		this(type, id, id, batchId, label, subtype);
	}

	public CmfObject(CmfType type, String id, String searchKey, String batchId, String label, String subtype) {
		if (type == null) { throw new IllegalArgumentException("Must provide a valid object type"); }
		if (id == null) { throw new IllegalArgumentException("Must provide a valid object id"); }
		if (label == null) { throw new IllegalArgumentException("Must provide a valid object label"); }
		if (subtype == null) { throw new IllegalArgumentException("Must provide a valid object subtype"); }
		this.type = type;
		this.id = id;
		this.searchKey = Tools.coalesce(searchKey, id);
		this.batchId = Tools.coalesce(batchId, id);
		this.label = label;
		this.subtype = subtype;
	}

	final String getRelativeStreamLocation() {
		return this.relativeStreamLocation;
	}

	final void setRelativeStreamLocation(String relativeLocation) {
		this.relativeStreamLocation = relativeLocation;
	}

	public final CmfType getType() {
		return this.type;
	}

	public final String getSubtype() {
		return this.subtype;
	}

	public final String getId() {
		return this.id;
	}

	public final String getSearchKey() {
		return this.searchKey;
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

	public final CmfAttribute<V> getAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to retrieve"); }
		return this.attributes.get(name);
	}

	public final CmfAttribute<V> setAttribute(CmfAttribute<V> attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to set"); }
		return this.attributes.put(attribute.getName(), attribute);
	}

	public final CmfAttribute<V> removeAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to remove"); }
		return this.attributes.remove(name);
	}

	public final Collection<CmfAttribute<V>> getAttributes() {
		return Collections.unmodifiableCollection(this.attributes.values());
	}

	public final void setAttributes(Collection<CmfAttribute<V>> attributes) {
		this.attributes.clear();
		for (CmfAttribute<V> att : attributes) {
			setAttribute(att);
		}
	}

	public final int getPropertyCount() {
		return this.properties.size();
	}

	public final Set<String> getPropertyNames() {
		return Collections.unmodifiableSet(this.properties.keySet());
	}

	public final CmfProperty<V> getProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to retrieve"); }
		return this.properties.get(name);
	}

	public final CmfProperty<V> setProperty(CmfProperty<V> property) {
		if (property == null) { throw new IllegalArgumentException("Must provide a property to set"); }
		return this.properties.put(property.getName(), property);
	}

	public final CmfProperty<V> removeProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to remove"); }
		return this.properties.remove(name);
	}

	public final Collection<CmfProperty<V>> getProperties() {
		return Collections.unmodifiableCollection(this.properties.values());
	}

	public final void setProperties(Collection<CmfProperty<V>> properties) {
		this.properties.clear();
		for (CmfProperty<V> prop : properties) {
			setProperty(prop);
		}
	}

	public final CmfACL<V> getAcl() {
		return this.acl;
	}

	public final void setAcl(CmfACL<V> acl) {
		this.acl = acl;
	}

	protected String toStringTrailer() {
		return "";
	}

	@Override
	public final String toString() {
		final String trailer = toStringTrailer();
		final String trailerSep = ((trailer != null) && (trailer.length() > 0) ? ", " : "");
		return String.format("%s [type=%s, subtype=%s, id=%s, searchKey=%s, batchId=%s, label=%s%s%s]", getClass()
			.getSimpleName(), this.type, this.subtype, this.id, this.searchKey, this.batchId, this.label, trailerSep,
			trailer);
	}
}