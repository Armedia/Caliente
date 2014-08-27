package com.delta.cmsmf.mainEngine;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.exception.CMSMFFatalException;
import com.delta.cmsmf.properties.CMSMFProperties;
import com.delta.cmsmf.properties.PropertiesManager;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public abstract class CMSMFMain {

	/** The logger object used for logging. */
	protected final Logger logger = Logger.getLogger(getClass());

	private static CMSMFMain instance = null;

	/** The directory location where stream files will be created. */
	protected final File streamFilesDirectoryLocation;

	/** The directory location where content files will be created. */
	protected final File contentFilesDirectoryLocation;

	protected final boolean testMode;

	CMSMFMain() throws Throwable {
		// First things first...
		CMSMFMain.instance = this;

		// Convert the command-line parameters into configuration properties
		Properties parameters = new Properties();
		for (CLIParam p : CLIParam.values()) {
			String value = CMSMFLauncher.getParameter(p);
			if ((value != null) && (p.property != null)) {
				final String key = p.property.name;
				if ((key != null) && (value != null)) {
					parameters.setProperty(key, value);
				}
			}
		}

		this.logger.info(String.format("Launching CMSMF %s mode%n", CMSMFLauncher.getParameter(CLIParam.mode)));

		// If we have command-line parameters, these supersede all other configurations, even if
		// we have a configuration file explicitly listed.
		if (!parameters.isEmpty()) {
			PropertiesManager.addPropertySource(parameters);
		}

		// A configuration file has been specifed, so use its values ahead of the defaults
		if (CMSMFLauncher.getParameter(CLIParam.cfg) != null) {
			PropertiesManager.addPropertySource(CMSMFLauncher.getParameter(CLIParam.cfg));
		}

		// Finally, the catch-all, default configuration
		PropertiesManager.addPropertySource(CMSMFAppConstants.FULLY_QUALIFIED_CONFIG_FILE_NAME);
		PropertiesManager.init();

		this.testMode = (CMSMFLauncher.getParameter(CLIParam.test) != null);

		// Set the filesystem location where files will be created or read from
		this.streamFilesDirectoryLocation = new File(CMSMFProperties.STREAMS_DIRECTORY.getString()).getCanonicalFile();
		this.logger.info(String.format("Using streams directory: [%s]", this.contentFilesDirectoryLocation));

		// Set the filesystem location where the content files will be created or read from
		this.contentFilesDirectoryLocation = new File(CMSMFProperties.CONTENT_DIRECTORY.getString()).getCanonicalFile();
		this.logger.info(String.format("Using content directory: [%s]", this.contentFilesDirectoryLocation));

		start();
	}

	public static CMSMFMain getInstance() {
		return CMSMFMain.instance;
	}

	public File getStreamFilesDirectory() {
		return this.streamFilesDirectoryLocation;
	}

	public File getContentFilesDirectory() {
		return this.contentFilesDirectoryLocation;
	}

	public boolean isTestMode() {
		return this.testMode;
	}

	protected abstract void run() throws IOException, CMSMFFatalException;

	/**
	 * Starts the main processing of the application. It checks the properties
	 * file to see if a user has selected export step or import step. It accordingly
	 * establishes a session with either source repository or target repository.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void start() throws Throwable {
		run();
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.debug("##### CMS Migration Process finished #####");
		}
	}

}