package com.armedia.cmf.storage;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.armedia.commons.utilities.Tools;

public final class CmfACL<V> implements Serializable {
	private static final long serialVersionUID = 1L;

	public static enum AccessorType {
		//
		USER,
		GROUP,
		ROLE,
		//
		;
	}

	private final String identifier;
	private String storedIdentifier = null;
	private final Set<CmfAccessor> allAccessors;
	private final Map<AccessorType, Map<String, CmfAccessor>> accessors;
	private final Map<String, CmfProperty<V>> properties;

	public CmfACL() {
		this(null, null);
	}

	public CmfACL(String identifier) {
		this(identifier, null);
	}

	public CmfACL(Set<CmfAccessor> accessors) {
		this(null, accessors);
	}

	public CmfACL(String identifier, Set<CmfAccessor> accessors) {
		this.identifier = identifier;
		if (identifier != null) {
			this.storedIdentifier = identifier;
		}
		this.accessors = new EnumMap<AccessorType, Map<String, CmfAccessor>>(AccessorType.class);
		this.allAccessors = new TreeSet<CmfAccessor>();
		this.properties = new TreeMap<String, CmfProperty<V>>();

		if ((accessors == null) || accessors.isEmpty()) { return; }

		this.allAccessors.addAll(accessors);

		for (CmfAccessor a : this.allAccessors) {
			Map<String, CmfAccessor> m = this.accessors.get(a.getAccessorType());
			if (m == null) {
				m = new TreeMap<String, CmfAccessor>();
				this.accessors.put(a.getAccessorType(), m);
			}
			m.put(a.getName(), a);
		}
	}

	public String getIdentifier() {
		return this.identifier;
	}

	void setStoredIdentifier(String identifier) {
		this.storedIdentifier = identifier;
	}

	public String getStoredIdentifier() {
		return this.storedIdentifier;
	}

	public int getAccessorCount() {
		return this.allAccessors.size();
	}

	public boolean hasAccessors() {
		return !this.allAccessors.isEmpty();
	}

	public Set<CmfAccessor> getAccessors() {
		return new TreeSet<CmfAccessor>(this.allAccessors);
	}

	public Set<AccessorType> getAccessorTypes() {
		return EnumSet.copyOf(this.accessors.keySet());
	}

	public int getAccessorCount(AccessorType type) {
		Map<String, CmfAccessor> m = this.accessors.get(type);
		if (m == null) { return 0; }
		return m.size();
	}

	public boolean hasAccessors(AccessorType type) {
		return (this.accessors.get(type) != null);
	}

	public Set<CmfAccessor> getAccessors(AccessorType type) {
		Map<String, CmfAccessor> m = this.accessors.get(type);
		if (m == null) { return new TreeSet<CmfAccessor>(); }
		return new TreeSet<CmfAccessor>(m.values());
	}

	public Set<String> getAccessorNames(AccessorType type) {
		Map<String, CmfAccessor> m = this.accessors.get(type);
		if (m == null) { return Collections.emptySet(); }
		return m.keySet();
	}

	public boolean hasAccessor(AccessorType type, String name) {
		Map<String, CmfAccessor> m = this.accessors.get(type);
		return (m != null) && m.containsKey(name);
	}

	public CmfAccessor getAccessor(AccessorType type, String name) {
		Map<String, CmfAccessor> m = this.accessors.get(type);
		if (m == null) { return null; }
		return m.get(name);
	}

	public CmfAccessor addAccessor(CmfAccessor accessor) {
		Map<String, CmfAccessor> m = this.accessors.get(accessor.getAccessorType());
		if (m == null) {
			m = new TreeMap<String, CmfAccessor>();
			this.accessors.put(accessor.getAccessorType(), m);
		}
		CmfAccessor ret = m.put(accessor.getName(), accessor);
		this.allAccessors.add(accessor);
		return ret;
	}

	public CmfAccessor removeAccessor(AccessorType type, String name) {
		Map<String, CmfAccessor> m = this.accessors.get(type);
		if (m == null) { return null; }
		CmfAccessor ret = m.remove(name);
		this.allAccessors.remove(ret);
		return ret;
	}

	public Set<String> getPropertyNames() {
		return new TreeSet<String>(this.properties.keySet());
	}

	public int getPropertyCount() {
		return this.properties.size();
	}

	public boolean hasProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name"); }
		return this.properties.containsKey(name);
	}

	public CmfProperty<V> getProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name"); }
		return this.properties.get(name);
	}

	public CmfProperty<V> setProperty(CmfProperty<V> property) {
		if (property == null) { throw new IllegalArgumentException("Must provide a property to set"); }
		return this.properties.put(property.getName(), property);
	}

	public CmfProperty<V> clearProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name"); }
		return this.properties.remove(name);
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.identifier, this.properties, this.allAccessors);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfACL<?> other = CmfACL.class.cast(obj);
		if (!Tools.equals(this.identifier, other.identifier)) { return false; }
		if (!Tools.equals(this.properties, other.properties)) { return false; }
		if (!Tools.equals(this.allAccessors, other.allAccessors)) { return false; }
		return true;
	}
}