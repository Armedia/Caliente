package com.delta.cmsmf.cfg;

import java.io.File;
import java.net.URL;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

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
	DB_DIRECTORY("db.directory"),
	DEFAULT_USER_PASSWORD("default.user.password"),
	OWNER_ATTRIBUTES("owner.attributes"),
	MAIL_RECIPIENTS("mail.recipients"),
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
	STATE_CABINET("state.cabinet"),
	EXPORT_BATCH_SIZE("export.batch.size"),
	JDBC_DRIVER("jdbc.driver"),
	JDBC_URL("jdbc.url"),
	JDBC_USER("jdbc.user"),
	JDBC_PASSWORD("jdbc.password"),
	MANIFEST_OUTCOMES("manifest.outcomes"),
	MANIFEST_TYPES("manifest.types"),

	//
	CMF_EXCLUDE_TYPES("cmf.exclude.types"),

	// END OF LIST
	;

	public final String name;

	private Setting(String name) {
		this.name = name;
	}

	private static final String DEFAULT_PROPERTIES = "default.properties";

	private static Configuration DEFAULTS = null;

	private static synchronized Configuration getDefaults() {
		if (Setting.DEFAULTS == null) {
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
			Setting.DEFAULTS = def;
		}
		return Setting.DEFAULTS;
	}

	public int getInt() {
		return getInt(Setting.getDefaults().getInt(this.name, 0));
	}

	public int getInt(int altDefault) {
		return SettingManager.getProperty(this.name, altDefault);
	}

	public boolean getBoolean() {
		return getBoolean(Setting.getDefaults().getBoolean(this.name, false));
	}

	public boolean getBoolean(boolean altDefault) {
		return SettingManager.getProperty(this.name, altDefault);
	}

	public String getString() {
		return getString(Setting.getDefaults().getString(this.name, ""));
	}

	public String getString(String altDefault) {
		return SettingManager.getProperty(this.name, altDefault);
	}
}