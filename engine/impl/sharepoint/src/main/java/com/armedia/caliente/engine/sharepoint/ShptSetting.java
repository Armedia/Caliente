package com.armedia.caliente.engine.sharepoint;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfDataType;

public enum ShptSetting implements TransferEngineSetting {
	//
	URL(CmfDataType.STRING),
	USER(CmfDataType.STRING),
	PASSWORD(CmfDataType.STRING),
	DOMAIN(CmfDataType.STRING),
	EXCLUDE_EMPTY_FOLDERS(CmfDataType.STRING, "excludeEmptyFolders", Boolean.FALSE),
	//
	;

	private final String label;
	private final CmfDataType type;
	private final Object defaultValue;
	private final boolean required;

	private ShptSetting(CmfDataType type) {
		this(type, null);
	}

	private ShptSetting(CmfDataType type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private ShptSetting(CmfDataType type, Object defaultValue, boolean required) {
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