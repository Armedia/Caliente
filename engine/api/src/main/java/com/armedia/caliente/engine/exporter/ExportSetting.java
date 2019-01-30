package com.armedia.caliente.engine.exporter;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValue;

public enum ExportSetting implements TransferEngineSetting {
	//
	FROM(CmfValue.Type.STRING),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValue.Type type;
	private final boolean required;

	private ExportSetting(CmfValue.Type type) {
		this(type, null);
	}

	private ExportSetting(CmfValue.Type type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private ExportSetting(CmfValue.Type type, Object defaultValue, boolean required) {
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
	public CmfValue.Type getType() {
		return this.type;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}
}