package com.delta.cmsmf.cfg;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * The Class SettingManager reads the properties from cmsmf properties file and makes them available
 * during application execution. This class implements singleton design pattern to maintain single
 * set of properties through out the execution.
 * <p>
 * This class uses Apache commons configuration library to manage the properties.
 *
 * @author Shridev Makim 6/15/2010
 */
public class SettingManager {

	/**
	 * Instantiates a new properties manager. Private constructor to prevent new instances being
	 * created.
	 */
	private SettingManager() {
		// no code here; this is a singleton class so private constructor
	}

	private static final String DEFAULT_PROPERTIES = "default.properties";
	private static final AbstractConfiguration DEFAULTS;
	private static final List<AbstractConfiguration> CONFIGURATIONS = new ArrayList<AbstractConfiguration>();

	static {
		PropertiesConfiguration def = new PropertiesConfiguration();
		URL url = Thread.currentThread().getContextClassLoader().getResource(SettingManager.DEFAULT_PROPERTIES);
		if (url != null) {
			def.setDelimiterParsingDisabled(true);
			def.setListDelimiter('|');
			try {
				def.load(url);
			} catch (ConfigurationException e) {
				throw new RuntimeException(String.format("Failed to load the property defaults from [%s]",
					SettingManager.DEFAULT_PROPERTIES));
			}
		}
		// Load the defaults
		DEFAULTS = def;
	}

	private static AbstractConfiguration CFG = null;

	public static void addPropertySource(String propertyFilePath) throws ConfigurationException {
		if (propertyFilePath == null) { return; }
		SettingManager.addPropertySource(new File(propertyFilePath));
	}

	private static void configure(AbstractConfiguration cfg) {
		if (cfg != null) {
			cfg.setDelimiterParsingDisabled(true);
			cfg.setListDelimiter('|');
		}
	}

	public static void addPropertySource(URL propertyUrl) throws ConfigurationException {
		if (propertyUrl == null) { return; }
		PropertiesConfiguration cfg = new PropertiesConfiguration();
		SettingManager.configure(cfg);
		cfg.load(propertyUrl);
		SettingManager.addConfiguration(cfg);
	}

	public static void addPropertySource(File propertyFile) throws ConfigurationException {
		if (propertyFile == null) { return; }
		if (!propertyFile.exists()) {
			System.err.printf("Property file [%s] does not exist, ignoring%n", propertyFile.getAbsolutePath());
			return;
		}
		if (!propertyFile.isFile()) {
			System.err.printf("Property file [%s] is not a regular file, ignoring%n", propertyFile.getAbsolutePath());
			return;
		}
		if (!propertyFile.canRead()) {
			System.err.printf("Property file [%s] can't be read, ignoring%n", propertyFile.getAbsolutePath());
			return;
		}
		PropertiesConfiguration cfg = new PropertiesConfiguration();
		SettingManager.configure(cfg);
		cfg.load(propertyFile);
		// TODO: Support XML properties file format?
		SettingManager.addConfiguration(cfg);
	}

	public static void addPropertySource(Properties properties) throws ConfigurationException {
		if (properties == null) { return; }
		Properties props = new Properties();
		props.putAll(properties);
		MapConfiguration cfg = new MapConfiguration(props);
		SettingManager.configure(cfg);
		SettingManager.addConfiguration(cfg);
	}

	protected static synchronized void addConfiguration(AbstractConfiguration configuration) {
		if (SettingManager.CFG != null) { return; }
		if (configuration != null) {
			SettingManager.CONFIGURATIONS.add(configuration);
		}
	}

	public static synchronized void init() {
		if (SettingManager.CFG == null) {
			SettingManager.CFG = new MapConfiguration(new HashMap<String, Object>());

			SettingManager.CFG.copy(SettingManager.DEFAULTS);
			for (AbstractConfiguration c : SettingManager.CONFIGURATIONS) {
				SettingManager.CFG.copy(c);
			}

			SettingManager.CFG.getSubstitutor().setEnableSubstitutionInVariables(true);
			SettingManager.CFG.setDelimiterParsingDisabled(true);
			SettingManager.CFG.setListDelimiter('|');
		}
	}

	private static synchronized void ensureInitialized() {
		if (SettingManager.CFG == null) { throw new IllegalStateException(
			"The SettingsManager has not yet been initialized.  Make sure you call init() first"); }
	}

	/**
	 * Gets a string property value for a given proprty name from a property configuration.
	 *
	 * @param propName
	 *            the prop name
	 * @param defaultValue
	 *            the default value
	 * @return the string
	 */
	static String getString(String propName, String defaultValue) {
		SettingManager.ensureInitialized();
		return SettingManager.CFG.getString(propName, defaultValue);
	}

	static String getString(String propName) {
		return SettingManager.getString(propName, null);
	}

	/**
	 * Gets a integer property value for a given property name from a property configuration.
	 *
	 * @param propName
	 *            the prop name
	 * @param defaultValue
	 *            the default value
	 * @return the int
	 */
	static Integer getInteger(String propName, Integer defaultValue) {
		SettingManager.ensureInitialized();
		return SettingManager.CFG.getInt(propName, defaultValue);
	}

	static Integer getInteger(String propName) {
		return SettingManager.getInteger(propName, null);
	}

	/**
	 * Gets a long property value for a given property name from a property configuration.
	 *
	 * @param propName
	 *            the prop name
	 * @param defaultValue
	 *            the default value
	 * @return the long
	 */
	static Long getLong(String propName, Long defaultValue) {
		SettingManager.ensureInitialized();
		return SettingManager.CFG.getLong(propName, defaultValue);
	}

	static Long getLong(String propName) {
		return SettingManager.getLong(propName, null);
	}

	static Boolean getBoolean(String propName, Boolean defaultValue) {
		SettingManager.ensureInitialized();
		return SettingManager.CFG.getBoolean(propName, defaultValue);
	}

	static Boolean getBoolean(String propName) {
		return SettingManager.getBoolean(propName, null);
	}

	public static synchronized void close() {
		SettingManager.CONFIGURATIONS.clear();
		SettingManager.CFG = null;
	}
}