package com.armedia.caliente.engine.cmis;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValueType;

public enum CmisSetting implements TransferEngineSetting {
	//
	EXPORT_PAGE_SIZE(CmfValueType.INTEGER, 100),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValueType type;
	private final boolean required;

	private CmisSetting(CmfValueType type) {
		this(type, null);
	}

	private CmisSetting(CmfValueType type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private CmisSetting(CmfValueType type, Object defaultValue, boolean required) {
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
	public CmfValueType getType() {
		return this.type;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}
}