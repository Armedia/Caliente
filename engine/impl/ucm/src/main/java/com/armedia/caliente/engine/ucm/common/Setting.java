package com.armedia.caliente.engine.ucm.common;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum Setting implements ConfigurationSetting {
	//
	PATHS("paths"),
	//
	;

	public final String name;
	private final Object defaultValue;

	private Setting(String name) {
		this(name, null);
	}

	private Setting(String name, Object defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
	}

	@Override
	public String getLabel() {
		return this.name;
	}

	@Override
	public Object getDefaultValue() {
		return this.defaultValue;
	}
}