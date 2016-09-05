package com.armedia.cmf.engine.cmis;

import com.armedia.cmf.engine.TransferEngineSetting;
import com.armedia.cmf.storage.CmfDataType;

public enum CmisSetting implements TransferEngineSetting {
	//
	EXPORT_PATH(CmfDataType.STRING),
	EXPORT_ID(CmfDataType.STRING),
	EXPORT_QUERY(CmfDataType.STRING),
	EXPORT_PAGE_SIZE(CmfDataType.INTEGER, 100),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfDataType type;
	private final boolean required;

	private CmisSetting(CmfDataType type) {
		this(type, null);
	}

	private CmisSetting(CmfDataType type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private CmisSetting(CmfDataType type, Object defaultValue, boolean required) {
		this.label = name().toLowerCase();
		this.defaultValue = defaultValue;
		this.type = type;
		this.required = required;
	}

	@Override
	public final String getLabel() {
		return this.label;
	}

	@Override
	public final Object getDefaultValue() {
		return this.defaultValue;
	}

	@Override
	public CmfDataType getType() {
		return this.type;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}
}