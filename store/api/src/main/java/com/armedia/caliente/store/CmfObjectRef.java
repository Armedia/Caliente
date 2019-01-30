package com.armedia.caliente.store;

import java.io.Serializable;

import com.armedia.commons.utilities.Tools;

public class CmfObjectRef implements Comparable<CmfObjectRef>, Serializable {
	private static final long serialVersionUID = 1L;

	private final CmfArchetype type;
	private final String id;

	public CmfObjectRef(CmfObjectRef other) {
		if (other == null) { throw new IllegalArgumentException("Must provide another object to build from"); }
		this.type = other.type;
		this.id = other.id;
	}

	public CmfObjectRef(CmfArchetype type, String id) {
		if (type == null) { throw new IllegalArgumentException("Must provide the object's type"); }
		if (id == null) { throw new IllegalArgumentException("Must provide the object's ID"); }
		this.type = type;
		this.id = id;
	}

	public final CmfArchetype getType() {
		return this.type;
	}

	public final String getId() {
		return this.id;
	}

	public boolean isNull() {
		return (this.type == null) || (this.id == null);
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.type, this.id);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfObjectRef other = CmfObjectRef.class.cast(obj);
		if (this.type != other.type) { return false; }
		if (!Tools.equals(this.id, other.id)) { return false; }
		return true;
	}

	@Override
	public int compareTo(CmfObjectRef o) {
		if (o == null) { return 1; }
		int r = Tools.compare(this.type, o.type);
		if (r != 0) { return r; }
		r = Tools.compare(this.id, o.id);
		if (r != 0) { return r; }
		return 0;
	}

	@Override
	public String toString() {
		return String.format("CmfObjectRef [type=%s, id=%s]", this.type.name(), this.id);
	}

	public final String getShortLabel() {
		return String.format("%s[%s]", this.type.name(), this.id);
	}
}