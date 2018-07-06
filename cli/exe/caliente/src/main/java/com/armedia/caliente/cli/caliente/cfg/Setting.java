package com.armedia.caliente.cli.caliente.cfg;

import com.armedia.caliente.engine.importer.ImportSetting;

public enum Setting {
	//
	IMPORT_MAX_ERRORS("import.max.errors"),
	OWNER_ATTRIBUTES("owner.attributes"),
	SPECIAL_USERS("special.users"),
	SPECIAL_GROUPS("special.groups"),
	SPECIAL_TYPES("special.types"),
	SHPT_SOURCE_PREFIX("shpt.source.prefix"),

	USER_MAP("user.map"),
	GROUP_MAP("group.map"),
	ROLE_MAP("role.map"),
	FILENAME_MAP("filename.map"),

	//
	CMF_IMPORT_TARGET_LOCATION(ImportSetting.TARGET_LOCATION.getLabel()),
	CMF_IMPORT_TRIM_PREFIX(ImportSetting.TRIM_PREFIX.getLabel()),

	// END OF LIST
	;

	public final String name;

	private Setting(String name) {
		this.name = name;
	}
}