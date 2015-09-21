package com.armedia.cmf.engine.importer;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum ImportSetting implements ConfigurationSetting {
	//
	TARGET_LOCATION("/"),
	TRIM_PREFIX(0),
	IGNORE_CONTENT(false),
	//
	;

	private final String label;
	private final Object defaultValue;

	private ImportSetting() {
		this(null);
	}

	private ImportSetting(Object defaultValue) {
		this.label = String.format("cmf.import.%s", name().toLowerCase().replaceAll("_", "."));
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