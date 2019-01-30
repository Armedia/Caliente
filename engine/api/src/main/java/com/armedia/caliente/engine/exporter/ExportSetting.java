package com.armedia.caliente.engine.exporter;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValueType;

public enum ExportSetting implements TransferEngineSetting {
	//
	FROM(CmfValueType.STRING),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValueType type;
	private final boolean required;

	private ExportSetting(CmfValueType type) {
		this(type, null);
	}

	private ExportSetting(CmfValueType type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private ExportSetting(CmfValueType type, Object defaultValue, boolean required) {
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