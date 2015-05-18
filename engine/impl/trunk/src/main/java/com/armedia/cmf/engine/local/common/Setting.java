package com.armedia.cmf.engine.local.common;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum Setting implements ConfigurationSetting {
	//
	COPY_CONTENT,
	INCLUDE_ALL_VERSIONS,
	//
	;

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