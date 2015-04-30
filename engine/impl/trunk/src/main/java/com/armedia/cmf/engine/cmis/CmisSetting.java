package com.armedia.cmf.engine.cmis;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum CmisSetting implements ConfigurationSetting {
	//
	EXPORT_PATH(),
	EXPORT_QUERY(),
	EXPORT_PAGE_SIZE(100),
	//
	;

	private final String label;
	private final Object defaultValue;

	private CmisSetting() {
		this(null);
	}

	private CmisSetting(Object defaultValue) {
		this.label = name().toLowerCase().replace('_', '.');
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