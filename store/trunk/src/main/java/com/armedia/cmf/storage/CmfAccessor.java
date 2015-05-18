package com.armedia.cmf.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.armedia.cmf.storage.CmfACL.AccessorType;
import com.armedia.commons.utilities.Tools;

public final class CmfAccessor implements Serializable, Comparable<CmfAccessor> {
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_TYPE = "<default>";

	private final String name;
	private final CmfACL.AccessorType accessorType;

	private final int permissionCount;
	private final Map<String, Map<String, CmfPermission>> permissions;

	public CmfAccessor(String name, AccessorType accessorType, Collection<CmfPermission> permissions) {
		this.name = name;
		this.accessorType = accessorType;

		int count = 0;
		Set<String> types = new HashSet<String>();
		Map<String, Map<String, CmfPermission>> M = new TreeMap<String, Map<String, CmfPermission>>();
		for (CmfPermission p : permissions) {
			String type = p.getType();
			if (type == null) {
				type = CmfAccessor.DEFAULT_TYPE;
			}
			Map<String, CmfPermission> m = M.get(type);
			if (m == null) {
				m = new TreeMap<String, CmfPermission>();
				M.put(type, m);
				types.add(type);
			}
			m.put(p.getName(), p);
		}
		for (String t : types) {
			Map<String, CmfPermission> m = M.get(t);
			m = Tools.freezeMap(new LinkedHashMap<String, CmfPermission>(m));
			count += m.size();
			M.put(t, m);
		}
		this.permissions = Tools.freezeMap(new LinkedHashMap<String, Map<String, CmfPermission>>(M));
		this.permissionCount = count;
	}

	public String getName() {
		return this.name;
	}

	public CmfACL.AccessorType getAccessorType() {
		return this.accessorType;
	}

	public int getPermissionCount() {
		return this.permissionCount;
	}

	public Set<String> getPermissionTypes() {
		return this.permissions.keySet();
	}

	private String sanitizeType(String type) {
		return Tools.coalesce(type, CmfAccessor.DEFAULT_TYPE);
	}

	public Set<String> getPermissionNames(String type) {
		Map<String, CmfPermission> m = this.permissions.get(sanitizeType(type));
		if (m == null) { return Collections.emptySet(); }
		return m.keySet();
	}

	public int getPermissionCount(String type) {
		Map<String, CmfPermission> m = this.permissions.get(sanitizeType(type));
		if (m == null) { return 0; }
		return m.size();
	}

	public CmfPermission getPermission(String type, String name) {
		Map<String, CmfPermission> m = this.permissions.get(sanitizeType(type));
		if (m == null) { return null; }
		return m.get(name);
	}

	public Collection<CmfPermission> getPermissions(String type) {
		Map<String, CmfPermission> m = this.permissions.get(sanitizeType(type));
		if (m == null) { return Collections.emptySet(); }
		return m.values();
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.accessorType, this.name);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfAccessor other = CmfAccessor.class.cast(obj);
		if (this.accessorType != other.accessorType) { return false; }
		if (!Tools.equals(this.name, other.name)) { return false; }
		return true;
	}

	@Override
	public int compareTo(CmfAccessor o) {
		if (o == null) { return 1; }
		int i = 0;
		i = Tools.compare(this.accessorType, o.accessorType);
		if (i != 0) { return i; }
		i = Tools.compare(this.name, o.name);
		if (i != 0) { return i; }
		return 0;
	}

	@Override
	public String toString() {
		return String.format("CmfAccessor [name=%s, accessorType=%s, permissionCount=%s, permissions=%s]", this.name,
			this.accessorType, this.permissionCount, this.permissions);
	}

}