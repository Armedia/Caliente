package com.armedia.caliente.engine.cmis;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValue;

public enum CmisSetting implements TransferEngineSetting {
	//
	EXPORT_PAGE_SIZE(CmfValue.Type.INTEGER, 100),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValue.Type type;
	private final boolean required;

	private CmisSetting(CmfValue.Type type) {
		this(type, null);
	}

	private CmisSetting(CmfValue.Type type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private CmisSetting(CmfValue.Type type, Object defaultValue, boolean required) {
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