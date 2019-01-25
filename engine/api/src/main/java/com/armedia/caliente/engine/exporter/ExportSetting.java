package com.armedia.caliente.engine.exporter;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfDataType;

public enum ExportSetting implements TransferEngineSetting {
	//
	FROM(CmfDataType.STRING),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfDataType type;
	private final boolean required;

	private ExportSetting(CmfDataType type) {
		this(type, null);
	}

	private ExportSetting(CmfDataType type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private ExportSetting(CmfDataType type, Object defaultValue, boolean required) {
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