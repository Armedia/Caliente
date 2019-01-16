package com.armedia.caliente.engine.dfc.common;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum Setting implements ConfigurationSetting {
	//
	SOURCE("source"),
	IMPORT_MAX_ERRORS("import.max.errors", 1),
	DEFAULT_USER_PASSWORD("default.user.password"),
	OWNER_ATTRIBUTES("owner.attributes", ""),
	SPECIAL_USERS("special.users", ""),
	SPECIAL_GROUPS("special.groups", ""),
	SPECIAL_TYPES("special.types", ""),
	EXPORT_BATCH_SIZE("export.batch.size", 10000),
	//
	;

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