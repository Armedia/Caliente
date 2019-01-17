package com.armedia.caliente.store;

import com.armedia.commons.utilities.Tools;

public class CmfObjectSearchSpec extends CmfObjectRef {
	private static final long serialVersionUID = 1L;

	private final String searchKey;

	public CmfObjectSearchSpec(CmfType type, String id) {
		this(type, id, id);
	}

	public CmfObjectSearchSpec(CmfType type, String id, String searchKey) {
		super(type, id);
		this.searchKey = Tools.coalesce(searchKey, id);
	}

	protected CmfObjectSearchSpec(CmfObjectSearchSpec other) {
		super(other);
		this.searchKey = other.searchKey;
	}

	public final String getSearchKey() {
		return this.searchKey;
	}

	@Override
	public boolean isNull() {
		return (this.searchKey == null) || super.isNull();
	}

	@Override
	public String toString() {
		return String.format("CmfObjectSearchSpec [type=%s, id=%s, searchKey=%s]", getType().name(), getId(),
			getSearchKey());
	}
}