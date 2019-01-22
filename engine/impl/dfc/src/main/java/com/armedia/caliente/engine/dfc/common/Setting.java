package com.armedia.caliente.engine.dfc.common;

import java.util.Collections;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum Setting implements ConfigurationSetting {
	//
	SOURCE("source"),
	IMPORT_MAX_ERRORS("import.max.errors", 1),
	DEFAULT_USER_PASSWORD("default.user.password"),
	OWNER_ATTRIBUTES("owner.attributes", Collections.emptyList()),
	SPECIAL_USERS("special.users", Collections.emptyList()),
	SPECIAL_GROUPS("special.groups", Collections.emptyList()),
	SPECIAL_TYPES("special.types", Collections.emptyList()),
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