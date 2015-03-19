package com.armedia.cmf.engine;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum TransferEngineSetting implements ConfigurationSetting {
	//
	EXCLUDE_TYPES;

	private final String label;
	private final Object defaultValue;

	private TransferEngineSetting() {
		this(null);
	}

	private TransferEngineSetting(Object defaultValue) {
		this.label = String.format("cmf.%s", name().toLowerCase().replaceAll("_", "."));
		this.defaultValue = defaultValue;
	}

	@Override
	public final String getLabel() {
		return this.label;
	}

	@Override
	public final Object getDefaultValue() {
		return this.defaultValue;
	}
}