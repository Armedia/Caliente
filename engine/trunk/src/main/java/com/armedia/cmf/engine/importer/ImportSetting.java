package com.armedia.cmf.engine.importer;

import com.armedia.cmf.engine.TransferEngineSetting;
import com.armedia.cmf.storage.CmfDataType;

public enum ImportSetting implements TransferEngineSetting {
	//
	TARGET_LOCATION(CmfDataType.STRING, "/"), TRIM_PREFIX(CmfDataType.INTEGER, 0),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfDataType type;
	private final boolean required;

	private ImportSetting(CmfDataType type) {
		this(type, null);
	}

	private ImportSetting(CmfDataType type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private ImportSetting(CmfDataType type, Object defaultValue, boolean required) {
		this.label = String.format("cmf.import.%s", name().toLowerCase().replaceAll("_", "."));
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