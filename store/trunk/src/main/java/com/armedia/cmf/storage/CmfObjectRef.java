package com.armedia.cmf.storage;

import java.io.Serializable;

import com.armedia.commons.utilities.Tools;

public final class CmfObjectRef implements Comparable<CmfObjectRef>, Serializable {
	private static final long serialVersionUID = 1L;

	private final CmfType type;
	private final String id;

	public CmfObjectRef(CmfObject<?> object) {
		this.type = object.getType();
		this.id = object.getId();
	}

	public CmfObjectRef(CmfType parentType, String parentId) {
		this.type = parentType;
		this.id = parentId;
	}

	public CmfType getType() {
		return this.type;
	}

	public String getId() {
		return this.id;
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
}