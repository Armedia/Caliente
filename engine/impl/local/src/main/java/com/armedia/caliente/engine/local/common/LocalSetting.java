package com.armedia.caliente.engine.local.common;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValue;

public enum LocalSetting implements TransferEngineSetting {
	//
	ROOT(CmfValue.Type.STRING),
	COPY_CONTENT(CmfValue.Type.BOOLEAN, true),
	INCLUDE_ALL_VERSIONS(CmfValue.Type.BOOLEAN, false),
	IGNORE_EMPTY_FOLDERS(CmfValue.Type.BOOLEAN, false),
	FAIL_ON_COLLISIONS(CmfValue.Type.BOOLEAN, true),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValue.Type type;
	private final boolean required;

	private LocalSetting(CmfValue.Type type) {
		this(type, null);
	}

	private LocalSetting(CmfValue.Type type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private LocalSetting(CmfValue.Type type, Object defaultValue, boolean required) {
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