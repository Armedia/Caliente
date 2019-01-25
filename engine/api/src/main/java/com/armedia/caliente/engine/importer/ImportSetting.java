package com.armedia.caliente.engine.importer;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfDataType;

public enum ImportSetting implements TransferEngineSetting {
	//
	ATTRIBUTE_MAPPING(CmfDataType.STRING),
	RESIDUALS_PREFIX(CmfDataType.STRING),
	TARGET_LOCATION(CmfDataType.STRING, "/"),
	TRIM_PREFIX(CmfDataType.INTEGER, 0),
	NO_FILENAME_MAP(CmfDataType.BOOLEAN, false),
	FILENAME_MAP(CmfDataType.STRING),
	VALIDATE_REQUIREMENTS(CmfDataType.BOOLEAN, false),
	RESTRICT_TO(CmfDataType.STRING),
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