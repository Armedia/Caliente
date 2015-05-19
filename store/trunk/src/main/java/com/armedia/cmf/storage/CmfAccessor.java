package com.armedia.cmf.storage;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.armedia.cmf.storage.CmfACL.AccessorType;
import com.armedia.commons.utilities.Tools;

public final class CmfAccessor implements Serializable, Comparable<CmfAccessor> {
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_TYPE = "<default>";

	private final String name;
	private final CmfACL.AccessorType accessorType;

	private final Set<CmfPermission> allPermissions;
	private final Map<String, Map<String, CmfPermission>> permissions;

	public CmfAccessor(String name, AccessorType accessorType) {
		this(name, accessorType, null);
	}

	public CmfAccessor(String name, AccessorType accessorType, Set<CmfPermission> permissions) {
		this.name = name;
		this.accessorType = accessorType;
		this.allPermissions = new TreeSet<CmfPermission>();
		this.permissions = new TreeMap<String, Map<String, CmfPermission>>();

		for (CmfPermission p : permissions) {
			String type = p.getType();
			if (type == null) {
				type = CmfAccessor.DEFAULT_TYPE;
			}
			Map<String, CmfPermission> m = this.permissions.get(type);
			if (m == null) {
				m = new TreeMap<String, CmfPermission>();
				this.permissions.put(type, m);
			}
			m.put(p.getName(), p);
		}
	}

	public String getName() {
		return this.name;
	}

	public CmfACL.AccessorType getAccessorType() {
		return this.accessorType;
	}

	public int getPermissionCount() {
		return this.allPermissions.size();
	}

	public boolean hasPermissions() {
		return !this.allPermissions.isEmpty();
	}

	public Set<CmfPermission> getPermissions() {
		return new TreeSet<CmfPermission>(this.allPermissions);
	}

	public Set<String> getPermissionTypes() {
		return new TreeSet<String>(this.permissions.keySet());
	}

	private String sanitizeType(String type) {
		return Tools.coalesce(type, CmfAccessor.DEFAULT_TYPE);
	}

	public Set<String> getPermissionNames(String type) {
		Map<String, CmfPermission> m = this.permissions.get(sanitizeType(type));
		if (m == null) { return new TreeSet<String>(); }
		return new TreeSet<String>(m.keySet());
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

	public Set<CmfPermission> getPermissions(String type) {
		Map<String, CmfPermission> m = this.permissions.get(sanitizeType(type));
		if (m == null) { return new TreeSet<CmfPermission>(); }
		return new TreeSet<CmfPermission>(m.values());
	}

	public boolean addPermission(CmfPermission permission) {
		if (!this.allPermissions.add(permission)) { return false; }
		final String type = sanitizeType(permission.getType());
		Map<String, CmfPermission> m = this.permissions.get(type);
		if (m == null) {
			m = new TreeMap<String, CmfPermission>();
			this.permissions.put(type, m);
		}
		m.put(type, permission);
		return true;
	}

	public CmfPermission removePermission(CmfPermission permission) {
		if (!this.allPermissions.remove(permission)) { return null; }
		final String type = sanitizeType(permission.getType());
		Map<String, CmfPermission> m = this.permissions.get(type);
		if (m == null) { return null; }
		return m.remove(permission.getName());
	}

	public CmfPermission removePermission(String type, String name) {
		return removePermission(new CmfPermission(sanitizeType(type), name, true));
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
		return String.format("CmfAccessor [name=%s, accessorType=%s, permissions=%s]", this.name, this.accessorType,
			this.allPermissions);
	}

}