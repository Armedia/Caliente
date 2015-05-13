package com.armedia.cmf.storage.local;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum Setting implements ConfigurationSetting {
	//
	BASE_DIR,
	FORCE_SAFE_FILENAMES(Boolean.TRUE),
	SAFE_FILENAME_ENCODING("UTF-8"),
	URI_STRATEGY,
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