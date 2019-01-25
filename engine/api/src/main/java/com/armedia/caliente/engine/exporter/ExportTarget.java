package com.armedia.caliente.engine.exporter;

import com.armedia.caliente.store.CmfObjectSearchSpec;
import com.armedia.caliente.store.CmfType;

public final class ExportTarget extends CmfObjectSearchSpec {
	private static final long serialVersionUID = 1L;

	private final Long number;

	public ExportTarget(CmfObjectSearchSpec spec) {
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
		if (this.number != null) {
			return String.format("ExportTarget [type=%s, id=%s, searchKey=%s, number=%s]", getType().name(), getId(),
				getSearchKey(), this.number);
		}
		return String.format("ExportTarget [type=%s, id=%s, searchKey=%s]", getType().name(), getId(), getSearchKey());
	}
}