package com.armedia.caliente.engine.local.common;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValueType;

public enum LocalSetting implements TransferEngineSetting {
	//
	ROOT(CmfValueType.STRING),
	COPY_CONTENT(CmfValueType.BOOLEAN, true),
	INCLUDE_ALL_VERSIONS(CmfValueType.BOOLEAN, false),
	IGNORE_EMPTY_FOLDERS(CmfValueType.BOOLEAN, false),
	FAIL_ON_COLLISIONS(CmfValueType.BOOLEAN, true),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValueType type;
	private final boolean required;

	private LocalSetting(CmfValueType type) {
		this(type, null);
	}

	private LocalSetting(CmfValueType type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private LocalSetting(CmfValueType type, Object defaultValue, boolean required) {
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