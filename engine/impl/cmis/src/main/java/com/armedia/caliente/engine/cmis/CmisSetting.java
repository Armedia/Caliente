package com.armedia.caliente.engine.cmis;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfDataType;

public enum CmisSetting implements TransferEngineSetting {
	//
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