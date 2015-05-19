package com.armedia.cmf.storage;

import java.io.Serializable;

import com.armedia.commons.utilities.Tools;

public final class CmfPermission implements Serializable, Comparable<CmfPermission> {
	private static final long serialVersionUID = 1L;

	private final String type;
	private final String name;
	private final boolean granted;

	public CmfPermission(String name) {
		this(null, name, true);
	}

	public CmfPermission(String name, boolean granted) {
		this(null, name, granted);
	}

	public CmfPermission(String type, String name, boolean granted) {
		this.type = (type != null ? type.toUpperCase() : null);
		this.name = name.toUpperCase();
		this.granted = granted;
	}

	public String getType() {
		return this.type;
	}

	public String getName() {
		return this.name;
	}

	public boolean isGranted() {
		return this.granted;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.type, this.name);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfPermission other = CmfPermission.class.cast(obj);
		if (!Tools.equals(this.type, other.type)) { return false; }
		if (!Tools.equals(this.name, other.name)) { return false; }
		return true;
	}

	public boolean isIdentical(CmfPermission other) {
		if (!equals(other)) { return false; }
		return this.granted == other.granted;
	}

	@Override
	public int compareTo(CmfPermission o) {
		if (o == null) { return 1; }
		int i = 0;
		i = Tools.compare(this.type, o.type);
		if (i != 0) { return i; }
		i = Tools.compare(this.name, o.name);
		if (i != 0) { return i; }
		return 0;
	}

	@Override
	public String toString() {
		return String.format("CmfPermission [type=%s, name=%s, granted=%s]", this.type, this.name, this.granted);
	}
}