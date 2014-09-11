package com.delta.cmsmf.properties;

import java.net.URL;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public enum CMSMFProperties {
	//
	COMPRESSDATA_FLAG("cmsmf.app.compressdata.flag"),
	EXPORT_PREDICATE("cmsmf.app.export.predicate"),
	IMPORT_MAX_ERRORS("cmsmf.app.import.errorcount.threshold"),
	CONTENT_DIRECTORY("cmsmf.app.importexport.content.directory"),
	STREAMS_DIRECTORY("cmsmf.app.importexport.directory"),
	DEFAULT_USER_PASSWORD("cmsmf.app.inlinepassworduser.passwordvalue"),
	OWNER_ATTRIBUTES("cmsmf.app.repositoryowner.attributestocheck"),
	CONTENT_READ_BUFFER_SIZE("content_read_buffer_size"),
	MAIL_RECIPIENTS("cmsmf.app.mail.recipients"),
	MAIL_FROM_ADDX("cmsmf.app.mail.fromAddress"),
	MAIL_SMTP_HOST("cmsmf.app.mail.smtphost"),
	SKIP_ACLS("cmsmf.app.export.skip.acls"),
	SKIP_USERS("cmsmf.app.export.skip.users"),
	SKIP_GROUPS("cmsmf.app.export.skip.groups"),
	SKIP_VSTAMP("cmsmf.app.export.skip.vstamp"),
	POST_PROCESS_IMPORT("cmsmf.app.import.postprocess.flag"),
	STATE_CABINET_NAME("cmsmf.app.state.cabinet");

	public final String name;

	private CMSMFProperties(String name) {
		this.name = name;
	}

	private static final String DEFAULT_PROPERTIES = "default.properties";

	static final Configuration DEFAULTS;

	static {
		PropertiesConfiguration def = new PropertiesConfiguration();
		URL url = Thread.currentThread().getContextClassLoader().getResource(CMSMFProperties.DEFAULT_PROPERTIES);
		if (url != null) {
			def.setDelimiterParsingDisabled(true);
			def.setListDelimiter('|');
			try {
				def.load(url);
			} catch (ConfigurationException e) {
				throw new RuntimeException(String.format("Failed to load the property defaults from [%s]",
					CMSMFProperties.DEFAULT_PROPERTIES));
			}
		}
		// Load the defaults
		DEFAULTS = def;
	}

	public int getInt() {
		return getInt(CMSMFProperties.DEFAULTS.getInt(this.name, 0));
	}

	public int getInt(int altDefault) {
		return PropertiesManager.getProperty(this.name, altDefault);
	}

	public boolean getBoolean() {
		return getBoolean(CMSMFProperties.DEFAULTS.getBoolean(this.name, false));
	}

	public boolean getBoolean(boolean altDefault) {
		return PropertiesManager.getProperty(this.name, altDefault);
	}

	public String getString() {
		return getString(CMSMFProperties.DEFAULTS.getString(this.name, ""));
	}

	public String getString(String altDefault) {
		return PropertiesManager.getProperty(this.name, altDefault);
	}
}