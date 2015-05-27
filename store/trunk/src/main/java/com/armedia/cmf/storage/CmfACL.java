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

	private final String identifier;
	private String storedIdentifier = null;
	private final Set<CmfActor> allActors;
	private final Map<CmfActor.Type, Map<String, CmfActor>> actors;
	private final Map<String, CmfProperty<V>> properties;

	public CmfACL() {
		this(null, null);
	}

	public CmfACL(String identifier) {
		this(identifier, null);
	}

	public CmfACL(Set<CmfActor> actors) {
		this(null, actors);
	}

	public CmfACL(String identifier, Set<CmfActor> actors) {
		this.identifier = identifier;
		if (identifier != null) {
			this.storedIdentifier = identifier;
		}
		this.actors = new EnumMap<CmfActor.Type, Map<String, CmfActor>>(CmfActor.Type.class);
		this.allActors = new TreeSet<CmfActor>();
		this.properties = new TreeMap<String, CmfProperty<V>>();

		if ((actors == null) || actors.isEmpty()) { return; }

		this.allActors.addAll(actors);

		for (CmfActor a : this.allActors) {
			Map<String, CmfActor> m = this.actors.get(a.getType());
			if (m == null) {
				m = new TreeMap<String, CmfActor>();
				this.actors.put(a.getType(), m);
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

	public int getActorCount() {
		return this.allActors.size();
	}

	public boolean hasActors() {
		return !this.allActors.isEmpty();
	}

	public Set<CmfActor> getActors() {
		return new TreeSet<CmfActor>(this.allActors);
	}

	public Set<CmfActor.Type> getActorTypes() {
		return EnumSet.copyOf(this.actors.keySet());
	}

	public int getActorCount(CmfActor.Type type) {
		Map<String, CmfActor> m = this.actors.get(type);
		if (m == null) { return 0; }
		return m.size();
	}

	public boolean hasActors(CmfActor.Type type) {
		return (this.actors.get(type) != null);
	}

	public Set<CmfActor> getActors(CmfActor.Type type) {
		Map<String, CmfActor> m = this.actors.get(type);
		if (m == null) { return new TreeSet<CmfActor>(); }
		return new TreeSet<CmfActor>(m.values());
	}

	public Set<String> getActorNames(CmfActor.Type type) {
		Map<String, CmfActor> m = this.actors.get(type);
		if (m == null) { return Collections.emptySet(); }
		return m.keySet();
	}

	public boolean hasActor(CmfActor.Type type, String name) {
		Map<String, CmfActor> m = this.actors.get(type);
		return (m != null) && m.containsKey(name);
	}

	public CmfActor getActor(CmfActor.Type type, String name) {
		Map<String, CmfActor> m = this.actors.get(type);
		if (m == null) { return null; }
		return m.get(name);
	}

	public CmfActor addActor(CmfActor actor) {
		Map<String, CmfActor> m = this.actors.get(actor.getType());
		if (m == null) {
			m = new TreeMap<String, CmfActor>();
			this.actors.put(actor.getType(), m);
		}
		CmfActor ret = m.put(actor.getName(), actor);
		this.allActors.add(actor);
		return ret;
	}

	public CmfActor removeActor(CmfActor.Type type, String name) {
		Map<String, CmfActor> m = this.actors.get(type);
		if (m == null) { return null; }
		CmfActor ret = m.remove(name);
		this.allActors.remove(ret);
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
		return Tools.hashTool(this, null, this.identifier, this.properties, this.allActors);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfACL<?> other = CmfACL.class.cast(obj);
		if (!Tools.equals(this.identifier, other.identifier)) { return false; }
		if (!Tools.equals(this.properties, other.properties)) { return false; }
		if (!Tools.equals(this.allActors, other.allActors)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("CmfACL [identifier=%s, storedIdentifier=%s, allActors=%s, properties=%s]",
			this.identifier, this.storedIdentifier, this.allActors, this.properties);
	}
}