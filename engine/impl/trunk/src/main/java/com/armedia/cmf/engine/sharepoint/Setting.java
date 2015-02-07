package com.armedia.cmf.engine.sharepoint;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum Setting implements ConfigurationSetting {
	//
	URL("url"),
	USER("user"),
	PASSWORD("password"),
	DOMAIN("domain"),
	PATH("path"),
	EXCLUDE_EMPTY_FOLDERS("excludeEmptyFolders", Boolean.FALSE);

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