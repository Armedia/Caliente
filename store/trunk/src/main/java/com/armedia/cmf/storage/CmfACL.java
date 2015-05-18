package com.armedia.cmf.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.armedia.commons.utilities.Tools;

public final class CmfACL implements Serializable {
	private static final long serialVersionUID = 1L;

	public static enum AccessorType {
		//
		USER,
		GROUP,
		//
		;
	}

	private final String identifier;
	private final Set<CmfAccessor> allAccessors;
	private final Map<AccessorType, Map<String, CmfAccessor>> accessors;

	public CmfACL(String identifier, Set<CmfAccessor> accessors) {
		this.identifier = identifier;
		if ((accessors == null) || accessors.isEmpty()) {
			this.allAccessors = Collections.emptySet();
			this.accessors = Collections.emptyMap();
			return;
		}

		Set<CmfAccessor> all = new TreeSet<CmfAccessor>();
		all.addAll(accessors);
		this.allAccessors = Tools.freezeSet(new LinkedHashSet<CmfAccessor>(all));

		Map<AccessorType, Map<String, CmfAccessor>> M = new EnumMap<AccessorType, Map<String, CmfAccessor>>(
			AccessorType.class);
		for (CmfAccessor a : all) {
			Map<String, CmfAccessor> m = M.get(a.getAccessorType());
			if (m == null) {
				m = new TreeMap<String, CmfAccessor>();
				M.put(a.getAccessorType(), m);
			}
			m.put(a.getName(), a);
		}
		for (AccessorType t : AccessorType.values()) {
			Map<String, CmfAccessor> m = M.get(t);
			if ((m == null) || m.isEmpty()) {
				continue;
			}
			M.put(t, Tools.freezeMap(new LinkedHashMap<String, CmfAccessor>(m)));
		}
		this.accessors = Tools.freezeMap(M);
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public int getAccessorCount() {
		return this.allAccessors.size();
	}

	public boolean hasAccessors() {
		return !this.allAccessors.isEmpty();
	}

	public Set<AccessorType> getAccessorTypes() {
		return this.accessors.keySet();
	}

	public int getAccessorCount(AccessorType type) {
		Map<String, CmfAccessor> m = this.accessors.get(type);
		if (m == null) { return 0; }
		return m.size();
	}

	public boolean hasAccessors(AccessorType type) {
		return (this.accessors.get(type) != null);
	}

	public Collection<CmfAccessor> getAccessors(AccessorType type) {
		Map<String, CmfAccessor> m = this.accessors.get(type);
		if (m == null) { return Collections.emptySet(); }
		return m.values();
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

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.identifier, this.allAccessors);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfACL other = CmfACL.class.cast(obj);
		if (!Tools.equals(this.identifier, other.identifier)) { return false; }
		if (!Tools.equals(this.accessors, other.accessors)) { return false; }
		return true;
	}
}