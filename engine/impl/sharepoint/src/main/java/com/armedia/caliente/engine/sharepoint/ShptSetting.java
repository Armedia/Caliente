package com.armedia.caliente.engine.sharepoint;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValue;

public enum ShptSetting implements TransferEngineSetting {
	//
	URL(CmfValue.Type.STRING),
	USER(CmfValue.Type.STRING),
	PASSWORD(CmfValue.Type.STRING),
	DOMAIN(CmfValue.Type.STRING),
	EXCLUDE_EMPTY_FOLDERS(CmfValue.Type.STRING, "excludeEmptyFolders", Boolean.FALSE),
	//
	;

	private final String label;
	private final CmfValue.Type type;
	private final Object defaultValue;
	private final boolean required;

	private ShptSetting(CmfValue.Type type) {
		this(type, null);
	}

	private ShptSetting(CmfValue.Type type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private ShptSetting(CmfValue.Type type, Object defaultValue, boolean required) {
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