package com.armedia.caliente.cli.caliente.cfg;

import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.importer.ImportSetting;

public enum Setting {
	//
	IMPORT_MAX_ERRORS("import.max.errors"),
	DEFAULT_USER_PASSWORD("default.user.password"),
	OWNER_ATTRIBUTES("owner.attributes"),
	MAIL_TO("mail.recipients"),
	MAIL_CC("mail.cc"),
	MAIL_BCC("mail.bcc"),
	MAIL_FROM_ADDX("mail.from"),
	MAIL_SMTP_HOST("mail.smtp.host"),
	MAIL_SMTP_PORT("mail.smtp.port"),
	SPECIAL_USERS("special.users"),
	SPECIAL_GROUPS("special.groups"),
	SPECIAL_TYPES("special.types"),
	SHPT_SOURCE_PREFIX("shpt.source.prefix"),

	USER_MAP("user.map"),
	GROUP_MAP("group.map"),
	ROLE_MAP("role.map"),
	FILENAME_MAP("filename.map"),

	//
	CMF_EXCLUDE_TYPES(TransferSetting.EXCLUDE_TYPES.getLabel()),
	CMF_IMPORT_TARGET_LOCATION(ImportSetting.TARGET_LOCATION.getLabel()),
	CMF_IMPORT_TRIM_PREFIX(ImportSetting.TRIM_PREFIX.getLabel()),

	// END OF LIST
	;

	public final String name;

	private Setting(String name) {
		this.name = name;
	}

	public int getInt() {
		return SettingManager.getInteger(this.name);
	}

	public int getInt(int altDefault) {
		return SettingManager.getInteger(this.name, altDefault);
	}

	public long getLong() {
		return SettingManager.getLong(this.name);
	}

	public long getLong(long altDefault) {
		return SettingManager.getLong(this.name, altDefault);
	}

	public boolean getBoolean() {
		return SettingManager.getBoolean(this.name);
	}

	public boolean getBoolean(boolean altDefault) {
		return SettingManager.getBoolean(this.name, altDefault);
	}

	public String getString() {
		return SettingManager.getString(this.name);
	}

	public String getString(String altDefault) {
		return SettingManager.getString(this.name, altDefault);
	}
}