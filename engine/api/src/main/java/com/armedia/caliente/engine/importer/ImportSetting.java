package com.armedia.caliente.engine.importer;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValue;

public enum ImportSetting implements TransferEngineSetting {
	//
	ATTRIBUTE_MAPPING(CmfValue.Type.STRING),
	RESIDUALS_PREFIX(CmfValue.Type.STRING),
	TARGET_LOCATION(CmfValue.Type.STRING, "/"),
	TRIM_PREFIX(CmfValue.Type.INTEGER, 0),
	NO_FILENAME_MAP(CmfValue.Type.BOOLEAN, false),
	FILENAME_MAP(CmfValue.Type.STRING),
	VALIDATE_REQUIREMENTS(CmfValue.Type.BOOLEAN, false),
	RESTRICT_TO(CmfValue.Type.STRING),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValue.Type type;
	private final boolean required;

	private ImportSetting(CmfValue.Type type) {
		this(type, null);
	}

	private ImportSetting(CmfValue.Type type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private ImportSetting(CmfValue.Type type, Object defaultValue, boolean required) {
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