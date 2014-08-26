package com.delta.cmsmf.properties;

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
import org.apache.log4j.Logger;

import com.delta.cmsmf.constants.CMSMFProperties;

/**
 * The Class PropertiesManager reads the properties from cmsmf properties file and makes them
 * available during
 * application execution. This class implements singleton design pattern to maintain single set of
 * properties
 * through out the execution.
 * <p>
 * This class uses Apache commons configuration library to manage the properties.
 *
 * @author Shridev Makim 6/15/2010
 */
public class PropertiesManager {

	protected static final Logger logger = Logger.getLogger(PropertiesManager.class);

	/**
	 * Instantiates a new properties manager. Private constructor to prevent
	 * new instances being created.
	 */
	private PropertiesManager() {
		// no code here; this is a singleton class so private constructor
	}

	private static final List<Configuration> CONFIGURATIONS = new ArrayList<Configuration>();

	private static Configuration CFG = null;

	public static void addPropertySource(String propertyFilePath) throws ConfigurationException {
		if (propertyFilePath == null) { return; }
		PropertiesManager.addPropertySource(new File(propertyFilePath));
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
		PropertiesManager.configure(cfg);
		cfg.load(propertyUrl);
		PropertiesManager.addConfiguration(cfg);
	}

	public static void addPropertySource(File propertyFile) throws ConfigurationException {
		if (propertyFile == null) { return; }
		if (!propertyFile.exists()) {
			PropertiesManager.logger.warn(String.format("Property file [%s] does not exist, ignoring",
				propertyFile.getAbsolutePath()));
			return;
		}
		if (!propertyFile.isFile()) {
			PropertiesManager.logger.warn(String.format("Property file [%s] is not a regular file, ignoring",
				propertyFile.getAbsolutePath()));
			return;
		}
		if (!propertyFile.canRead()) {
			PropertiesManager.logger.warn(String.format("Property file [%s] can't be read, ignoring",
				propertyFile.getAbsolutePath()));
			return;
		}
		PropertiesConfiguration cfg = new PropertiesConfiguration();
		PropertiesManager.configure(cfg);
		cfg.load(propertyFile);
		// TODO: Support XML properties file format?
		PropertiesManager.addConfiguration(cfg);
	}

	public static void addPropertySource(Properties properties) throws ConfigurationException {
		if (properties == null) { return; }
		MapConfiguration cfg = new MapConfiguration(new HashMap<Object, Object>(properties));
		PropertiesManager.configure(cfg);
		PropertiesManager.addConfiguration(cfg);
	}

	protected static synchronized void addConfiguration(AbstractConfiguration configuration) {
		if (PropertiesManager.CFG != null) { return; }
		if (configuration != null) {
			PropertiesManager.CONFIGURATIONS.add(configuration);
		}
	}

	public static synchronized void init() throws ConfigurationException {
		if (PropertiesManager.CFG == null) {
			PropertiesManager.CFG = new CompositeConfiguration(PropertiesManager.CONFIGURATIONS);
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
	public static String getProperty(CMSMFProperties propName, String defaultValue) {
		return PropertiesManager.CFG.getString(propName.name, defaultValue);
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
	public static int getProperty(CMSMFProperties propName, int defaultValue) {
		return PropertiesManager.CFG.getInt(propName.name, defaultValue);
	}

	public static boolean getProperty(CMSMFProperties propName, boolean defaultValue) {
		return PropertiesManager.CFG.getBoolean(propName.name, defaultValue);
	}

	public static List<Configuration> getConfigurations() {
		return PropertiesManager.CONFIGURATIONS;
	}

	public static synchronized void close() {
		PropertiesManager.CONFIGURATIONS.clear();
		PropertiesManager.CFG = null;
	}
}