package com.armedia.caliente.store.local;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum LocalContentStoreSetting implements ConfigurationSetting {
	//
	BASE_DIR,
	URI_ORGANIZER,
	FORCE_SAFE_FILENAMES(true),
	SAFE_FILENAME_ENCODING("UTF-8"),
	FIX_FILENAMES(false),
	FAIL_ON_COLLISIONS(false),
	IGNORE_DESCRIPTOR(false),
	USE_WINDOWS_FIX(false),
	STORE_PROPERTIES(true),
	//
	;

	private final String label;
	private final Object defaultValue;

	private LocalContentStoreSetting() {
		this(null);
	}

	private LocalContentStoreSetting(Object defaultValue) {
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