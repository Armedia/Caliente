package com.armedia.caliente.engine.sharepoint;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValueType;

public enum ShptSetting implements TransferEngineSetting {
	//
	URL(CmfValueType.STRING),
	USER(CmfValueType.STRING),
	PASSWORD(CmfValueType.STRING),
	DOMAIN(CmfValueType.STRING),
	EXCLUDE_EMPTY_FOLDERS(CmfValueType.STRING, "excludeEmptyFolders", Boolean.FALSE),
	//
	;

	private final String label;
	private final CmfValueType type;
	private final Object defaultValue;
	private final boolean required;

	private ShptSetting(CmfValueType type) {
		this(type, null);
	}

	private ShptSetting(CmfValueType type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private ShptSetting(CmfValueType type, Object defaultValue, boolean required) {
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