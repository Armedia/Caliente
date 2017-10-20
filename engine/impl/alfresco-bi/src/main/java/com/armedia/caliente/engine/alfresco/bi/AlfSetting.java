package com.armedia.caliente.engine.alfresco.bi;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum AlfSetting implements ConfigurationSetting {
	//
	ROOT, //
	DB, //
	CONTENT, //
	CONTENT_MODEL, //
	USER_MAP, //
	GROUP_MAP, //
	ROLE_MAP, //
	ATTRIBUTE_MAP, //
	RESIDUALS_PREFIX, //
	//
	;

	private final String label;
	private final Object defaultValue;

	private AlfSetting() {
		this(null);
	}

	private AlfSetting(Object defaultValue) {
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