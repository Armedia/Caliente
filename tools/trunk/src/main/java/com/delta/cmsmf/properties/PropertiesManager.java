package com.delta.cmsmf.properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

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

	/**
	 * Gets the singleton instance of properties manager class.
	 * 
	 * @return the properties manager
	 */
	public static synchronized PropertiesManager getPropertiesManager() {
		if (PropertiesManager.singletonInstance == null) {
			// we can call this private constructor
			PropertiesManager.singletonInstance = new PropertiesManager();
		}
		return PropertiesManager.singletonInstance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
		// prevent generation of a clone
	}

	/** The singleton instance. */
	private static PropertiesManager singletonInstance;

	/**
	 * Loads properties from the file detonated by file name. The named file
	 * should be located in a path defined in classpath.
	 * 
	 * @param fileName
	 *            the file name
	 * @throws ConfigurationException
	 *             the configuration exception
	 */
	public void loadProperties(String fileName) throws ConfigurationException {
		this.propConfig = new PropertiesConfiguration();
		// Disable parsing of the property value by delimiter. We do not have properties with
		// multiple values for now. The default delimiting character is ','. We are overriding it
		// with '|' to disable parsing
		this.propConfig.setListDelimiter('|');
		this.propConfig.load(fileName);
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
	public String getProperty(String propName, String defaultValue) {
		return this.propConfig.getString(propName, defaultValue);
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
	public int getProperty(String propName, int defaultValue) {
		return this.propConfig.getInt(propName, defaultValue);
	}

	/** The prop config. */
	private PropertiesConfiguration propConfig = null;

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 * @throws ConfigurationException
	 *             the configuration exception
	 */
	public static void main(String[] args) throws ConfigurationException {
		// This main method is not supposed to be invoked outside of IDE.
		// This is mainly for testing purposes

		System.out.println("started");
		PropertiesManager pm = PropertiesManager.getPropertiesManager();

		pm.loadProperties("config/CMSMF_app.properties");
		System.out.println(pm.getProperty("content_read_buffer_size", "defaultValue"));

		System.out.println("finished");

	}

}
