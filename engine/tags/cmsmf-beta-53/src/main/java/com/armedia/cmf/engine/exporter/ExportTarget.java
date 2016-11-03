package com.armedia.cmf.engine.exporter;

import com.armedia.cmf.storage.CmfObjectSpec;
import com.armedia.cmf.storage.CmfType;

public final class ExportTarget extends CmfObjectSpec {
	private static final long serialVersionUID = 1L;

	private final Long number;

	ExportTarget() {
		super();
		this.number = null;
	}

	public ExportTarget(CmfObjectSpec spec) {
		super(spec);
		this.number = null;
	}

	public ExportTarget(CmfType type, String id, String searchKey) {
		this(type, id, searchKey, null);
	}

	public ExportTarget(CmfType type, String id, String searchKey, Long number) {
		super(type, id, searchKey);
		this.number = number;
	}

	public Long getNumber() {
		return this.number;
	}

	@Override
	public String toString() {
		if (this.number != null) { return String.format("ExportTarget [type=%s, id=%s, searchKey=%s, number=%s]",
			getType().name(), getId(), getSearchKey(), this.number); }
		return String.format("ExportTarget [type=%s, id=%s, searchKey=%s]", getType().name(), getId(), getSearchKey());
	}
}