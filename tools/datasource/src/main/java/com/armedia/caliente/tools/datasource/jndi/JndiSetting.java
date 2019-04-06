package com.armedia.caliente.tools.datasource.jndi;

import com.armedia.commons.utilities.ConfigurationSetting;

enum JndiSetting implements ConfigurationSetting {
	//
	DATASOURCE_NAME;

	private final String label;
	private final Object defaultValue;

	private JndiSetting() {
		this(null);
	}

	private JndiSetting(Object defaultValue) {
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