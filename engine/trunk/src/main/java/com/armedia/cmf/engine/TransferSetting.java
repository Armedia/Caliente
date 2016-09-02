package com.armedia.cmf.engine;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum TransferSetting implements ConfigurationSetting {
	//
	EXCLUDE_TYPES, IGNORE_CONTENT(false), THREAD_COUNT, BACKLOG_SIZE, LATEST_ONLY(false),
	//
	;

	private final String label;
	private final Object defaultValue;

	private TransferSetting() {
		this(null);
	}

	private TransferSetting(Object defaultValue) {
		this.label = String.format("cmf.%s", name().toLowerCase().replaceAll("_", "."));
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