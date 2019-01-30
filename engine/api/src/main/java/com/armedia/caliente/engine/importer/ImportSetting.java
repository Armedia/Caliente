package com.armedia.caliente.engine.importer;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValueType;

public enum ImportSetting implements TransferEngineSetting {
	//
	ATTRIBUTE_MAPPING(CmfValueType.STRING),
	RESIDUALS_PREFIX(CmfValueType.STRING),
	TARGET_LOCATION(CmfValueType.STRING, "/"),
	TRIM_PREFIX(CmfValueType.INTEGER, 0),
	NO_FILENAME_MAP(CmfValueType.BOOLEAN, false),
	FILENAME_MAP(CmfValueType.STRING),
	VALIDATE_REQUIREMENTS(CmfValueType.BOOLEAN, false),
	RESTRICT_TO(CmfValueType.STRING),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValueType type;
	private final boolean required;

	private ImportSetting(CmfValueType type) {
		this(type, null);
	}

	private ImportSetting(CmfValueType type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private ImportSetting(CmfValueType type, Object defaultValue, boolean required) {
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