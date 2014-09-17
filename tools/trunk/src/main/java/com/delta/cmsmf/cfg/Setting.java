package com.delta.cmsmf.cfg;

import java.net.URL;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public enum Setting {
	//
	EXPORT_PREDICATE("export.predicate"),
	IMPORT_MAX_ERRORS("import.max.errors"),
	CONTENT_DIRECTORY("content.directory"),
	DB_DIRECTORY("db.directory"),
	DEFAULT_USER_PASSWORD("default.user.password"),
	OWNER_ATTRIBUTES("owner.attributes"),
	MAIL_RECIPIENTS("mail.recipients"),
	MAIL_FROM_ADDX("mail.fromAddress"),
	MAIL_SMTP_HOST("mail.smtphost"),
	THREADS("threads"),
	SKIP_ACLS("skip.acls"),
	SKIP_USERS("skip.users"),
	SKIP_GROUPS("skip.groups"),
	SPECIAL_USERS("special.users"),
	SPECIAL_GROUPS("special.groups"),
	SPECIAL_TYPES("special.types"),
	POST_PROCESS_IMPORT("import.postprocess"),
	STATE_CABINET_NAME("state.cabinet"),
	EXPORT_BATCH_SIZE("export.batch.size"),
	JDBC_DRIVER("jdbc.driver"),
	JDBC_URL("jdbc.url"),
	JDBC_USER("jdbc.user"),
	JDBC_PASSWORD("jdbc.password");

	public final String name;

	private Setting(String name) {
		this.name = name;
	}

	private static final String DEFAULT_PROPERTIES = "default.properties";

	static final Configuration DEFAULTS;

	static {
		PropertiesConfiguration def = new PropertiesConfiguration();
		URL url = Thread.currentThread().getContextClassLoader().getResource(Setting.DEFAULT_PROPERTIES);
		if (url != null) {
			def.setDelimiterParsingDisabled(true);
			def.setListDelimiter('|');
			try {
				def.load(url);
			} catch (ConfigurationException e) {
				throw new RuntimeException(String.format("Failed to load the property defaults from [%s]",
					Setting.DEFAULT_PROPERTIES));
			}
		}
		// Load the defaults
		DEFAULTS = def;
	}

	public int getInt() {
		return getInt(Setting.DEFAULTS.getInt(this.name, 0));
	}

	public int getInt(int altDefault) {
		return SettingManager.getProperty(this.name, altDefault);
	}

	public boolean getBoolean() {
		return getBoolean(Setting.DEFAULTS.getBoolean(this.name, false));
	}

	public boolean getBoolean(boolean altDefault) {
		return SettingManager.getProperty(this.name, altDefault);
	}

	public String getString() {
		return getString(Setting.DEFAULTS.getString(this.name, ""));
	}

	public String getString(String altDefault) {
		return SettingManager.getProperty(this.name, altDefault);
	}
}