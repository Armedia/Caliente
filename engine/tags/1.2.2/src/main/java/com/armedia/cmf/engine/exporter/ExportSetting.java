package com.armedia.cmf.engine.exporter;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum ExportSetting implements ConfigurationSetting {
	//

	//
	;

	private final String label;
	private final Object defaultValue;

	private ExportSetting() {
		this(null);
	}

	private ExportSetting(Object defaultValue) {
		this.label = String.format("cmf.export.%s", name().toLowerCase().replaceAll("_", "."));
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