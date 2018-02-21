package com.armedia.caliente.cli.caliente.cfg;

import java.io.File;

import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.importer.ImportSetting;

public enum Setting {
	//
	EXPORT_PREDICATE("export.predicate"),
	IMPORT_MAX_ERRORS("import.max.errors"),
	CONTENT_DIRECTORY("content.directory") {

		// By default, this goes to a subdirectory of the database
		// directory.
		@Override
		public String getString() {
			String str = super.getString(null);
			if (str != null) { return str; }
			str = DB_DIRECTORY.getString();
			return new File(new File(str), "content").getPath();
		}

	},
	CONTENT_ORGANIZATION("content.organization"),
	DB_DIRECTORY("db.directory"),
	DEFAULT_USER_PASSWORD("default.user.password"),
	OWNER_ATTRIBUTES("owner.attributes"),
	MAIL_TO("mail.recipients"),
	MAIL_CC("mail.cc"),
	MAIL_BCC("mail.bcc"),
	MAIL_FROM_ADDX("mail.from"),
	MAIL_SMTP_HOST("mail.smtp.host"),
	MAIL_SMTP_PORT("mail.smtp.port"),
	THREADS("threads"),
	SKIP_ACLS("skip.acls"),
	SKIP_USERS("skip.users"),
	SKIP_GROUPS("skip.groups"),
	SPECIAL_USERS("special.users"),
	SPECIAL_GROUPS("special.groups"),
	SPECIAL_TYPES("special.types"),
	POST_PROCESS_IMPORT("import.postprocess"),
	SHPT_SOURCE_PREFIX("shpt.source.prefix"),
	STATE_FOLDER("state.folder"),
	EXPORT_BATCH_SIZE("export.batch.size"),
	MANIFEST_OUTCOMES("manifest.outcomes"),
	MANIFEST_TYPES("manifest.types"),

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