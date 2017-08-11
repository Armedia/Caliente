package com.armedia.caliente.engine.local.common;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfDataType;

public enum LocalSetting implements TransferEngineSetting {
	//
	ROOT(CmfDataType.STRING),
	COPY_CONTENT(CmfDataType.BOOLEAN, true),
	INCLUDE_ALL_VERSIONS(CmfDataType.BOOLEAN, false),
	IGNORE_EMPTY_FOLDERS(CmfDataType.BOOLEAN, false),
	FAIL_ON_COLLISIONS(CmfDataType.BOOLEAN, true),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfDataType type;
	private final boolean required;

	private LocalSetting(CmfDataType type) {
		this(type, null);
	}

	private LocalSetting(CmfDataType type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private LocalSetting(CmfDataType type, Object defaultValue, boolean required) {
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