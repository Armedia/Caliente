package com.delta.cmsmf.cfg;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
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

	private static final List<Configuration> CONFIGURATIONS = new ArrayList<Configuration>();

	private static Configuration CFG = null;

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
		MapConfiguration cfg = new MapConfiguration(new HashMap<Object, Object>(properties));
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
			SettingManager.CFG = new CompositeConfiguration(SettingManager.CONFIGURATIONS);
		}
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
	static String getProperty(String propName, String defaultValue) {
		SettingManager.init();
		return SettingManager.CFG.getString(propName, defaultValue);
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
	static int getProperty(String propName, int defaultValue) {
		SettingManager.init();
		return SettingManager.CFG.getInt(propName, defaultValue);
	}

	static boolean getProperty(String propName, boolean defaultValue) {
		SettingManager.init();
		return SettingManager.CFG.getBoolean(propName, defaultValue);
	}

	public static List<Configuration> getConfigurations() {
		SettingManager.init();
		return SettingManager.CONFIGURATIONS;
	}

	public static synchronized void close() {
		SettingManager.CONFIGURATIONS.clear();
		SettingManager.CFG = null;
	}
}