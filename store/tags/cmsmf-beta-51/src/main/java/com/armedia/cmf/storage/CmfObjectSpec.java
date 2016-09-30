package com.armedia.cmf.storage;

import com.armedia.commons.utilities.Tools;

public class CmfObjectSpec {
	private final CmfType type;
	private final String id;
	private final String searchKey;

	public CmfObjectSpec(CmfType type, String id, String searchKey) {
		this.type = type;
		this.id = id;
		this.searchKey = searchKey;
	}

	protected CmfObjectSpec(CmfObjectSpec other) {
		if (other == null) { throw new IllegalArgumentException("Must provide another object to copy values from"); }
		this.type = other.type;
		this.id = other.id;
		this.searchKey = other.searchKey;
	}

	public CmfType getType() {
		return this.type;
	}

	public String getId() {
		return this.id;
	}

	public String getSearchKey() {
		return this.searchKey;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.type, this.id, this.searchKey);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfObjectSpec other = CmfObjectSpec.class.cast(obj);
		if (this.type != other.type) { return false; }
		if (!Tools.equals(this.id, other.id)) { return false; }
		if (!Tools.equals(this.searchKey, other.searchKey)) { return false; }
		return true;
	}
}