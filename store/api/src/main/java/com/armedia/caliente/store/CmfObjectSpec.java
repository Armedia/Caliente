package com.armedia.caliente.store;

import com.armedia.commons.utilities.Tools;

public class CmfObjectSpec extends CmfObjectRef {
	private static final long serialVersionUID = 1L;

	private final String searchKey;

	protected CmfObjectSpec() {
		super();
		this.searchKey = null;
	}

	public CmfObjectSpec(CmfType type, String id) {
		this(type, id, id);
	}

	public CmfObjectSpec(CmfType type, String id, String searchKey) {
		super(type, id);
		this.searchKey = Tools.coalesce(searchKey, id);
	}

	protected CmfObjectSpec(CmfObjectSpec other) {
		super(other);
		this.searchKey = other.searchKey;
	}

	public final String getSearchKey() {
		return this.searchKey;
	}

	@Override
	public String toString() {
		return String.format("CmfObjectSpec [type=%s, id=%s, searchKey=%s]", getType().name(), getId(), getSearchKey());
	}
}