package com.armedia.caliente.store.jdbc;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum Setting implements ConfigurationSetting {
	//
	LOCATION_TYPE, UPDATE_SCHEMA(true);

	private final String label;
	private final Object defaultValue;

	private Setting() {
		this(null);
	}

	private Setting(Object defaultValue) {
		String l = name();
		l = l.toLowerCase();
		l = l.replaceAll("_", ".");
		this.label = l;
		this.defaultValue = defaultValue;
	}

	@Override
	public String getLabel() {
		return this.label;
	}

	@Override
	public Object getDefaultValue() {
		return this.defaultValue;
	}
}