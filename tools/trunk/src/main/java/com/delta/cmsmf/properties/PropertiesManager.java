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

	public static void addPropertySource(URL propertyUrl) throws ConfigurationException {
		if (propertyUrl == null) { return; }
		PropertiesManager.addConfiguration(new PropertiesConfiguration(propertyUrl));
	}

	public static void addPropertySource(File propertyFile) throws ConfigurationException {
		if (propertyFile == null) { return; }
		// TODO: Support XML properties file format?
		PropertiesManager.addConfiguration(new PropertiesConfiguration(propertyFile));
	}

	public static void addPropertySource(Properties properties) throws ConfigurationException {
		if (properties == null) { return; }
		PropertiesManager.addConfiguration(new MapConfiguration(new HashMap<Object, Object>(properties)));
	}

	protected static synchronized void addConfiguration(AbstractConfiguration configuration) {
		if (PropertiesManager.CFG != null) { return; }
		if (configuration != null) {
			configuration.setDelimiterParsingDisabled(true);
			configuration.setListDelimiter('|');
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

	public static synchronized void close() {
		PropertiesManager.CFG = null;
	}
}