package com.armedia.caliente.tools.datasource.spring;

import com.armedia.commons.utilities.ConfigurationSetting;

enum SpringSetting implements ConfigurationSetting {
	//
	BEAN_NAME, //
	//
	;

	private final String label;
	private final Object defaultValue;

	private SpringSetting() {
		this(null);
	}

	private SpringSetting(Object defaultValue) {
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