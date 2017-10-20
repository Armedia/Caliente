package com.armedia.caliente.engine.ucm;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum UcmSetting implements ConfigurationSetting {
	//
	SOURCE("source"), //
	//
	;

	public final String name;
	private final Object defaultValue;

	private UcmSetting(String name) {
		this(name, null);
	}

	private UcmSetting(String name, Object defaultValue) {
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