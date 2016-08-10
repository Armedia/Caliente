package com.armedia.cmf.storage;

import com.armedia.commons.utilities.Tools;

public final class CmfObjectRef<V> {
	private final CmfType parentType;
	private final V parentId;

	public CmfObjectRef(CmfType parentType, V parentId) {
		this.parentType = parentType;
		this.parentId = parentId;
	}

	public CmfType getType() {
		return this.parentType;
	}

	public V getId() {
		return this.parentId;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.parentType, this.parentId);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfObjectRef<?> other = (CmfObjectRef<?>) obj;
		if (this.parentType != other.parentType) { return false; }
		if (!Tools.equals(this.parentId, other.parentId)) { return false; }
		return true;
	}
}