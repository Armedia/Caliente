package com.delta.cmsmf.mainEngine;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.constants.CMSMFProperties;
import com.delta.cmsmf.properties.PropertiesManager;
import com.documentum.fc.client.DfClient;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfLoginInfo;
import com.documentum.fc.common.IDfLoginInfo;
import com.documentum.fc.tools.RegistryPasswordUtils;

/**
 * The main method of this class is an entry point for the cmsmf application.
 * 
 * @author Shridev Makim 6/15/2010
 */
public abstract class CMSMFMain {

	/** The logger object used for logging. */
	protected final Logger logger = Logger.getLogger(getClass());

	private static CMSMFMain instance = null;

	/** The dctm session. */
	protected IDfSession dctmSession = null;

	/** The directory location where stream files will be created. */
	protected final File streamFilesDirectoryLocation;

	/** The directory location where content files will be created. */
	protected final File contentFilesDirectoryLocation;

	protected final boolean testMode;

	CMSMFMain() throws Throwable {

		// Convert the command-line parameters into configuration properties
		Properties parameters = new Properties();
		Map<CLIParam, String> cliArgs = CMSMFLauncher.getParsedCliArgs();
		for (CLIParam p : cliArgs.keySet()) {
			if (p.property != null) {
				final String key = p.property.name;
				final String value = cliArgs.get(p);
				if ((key != null) && (value != null)) {
					parameters.setProperty(key, value);
				}
			}
		}

		// If we have command-line parameters, these supersede all other configurations, even if
		// we have a configuration file explicitly listed.
		if (!parameters.isEmpty()) {
			PropertiesManager.addPropertySource(parameters);
		}

		// A configuration file has been specifed, so use its values ahead of the defaults
		if (cliArgs.containsKey(CLIParam.cfg.option.getLongOpt())) {
			PropertiesManager.addPropertySource(cliArgs.get(CLIParam.cfg));
		}

		// Finally, the catch-all, default configuration
		PropertiesManager.addPropertySource(CMSMFAppConstants.FULLY_QUALIFIED_CONFIG_FILE_NAME);
		PropertiesManager.init();

		this.testMode = cliArgs.containsKey(CLIParam.test);

		// Set the filesystem location where files will be created or read from
		this.streamFilesDirectoryLocation = new File(PropertiesManager.getProperty(CMSMFProperties.STREAMS_DIRECTORY,
			"")).getCanonicalFile();
		this.logger.info(String.format("Using streams directory: [%s]", this.contentFilesDirectoryLocation));

		// Set the filesystem location where the content files will be created or read from
		this.contentFilesDirectoryLocation = new File(PropertiesManager.getProperty(CMSMFProperties.CONTENT_DIRECTORY,
			"")).getCanonicalFile();
		this.logger.info(String.format("Using content directory: [%s]", this.contentFilesDirectoryLocation));

		start(cliArgs.get(CLIParam.docbase), cliArgs.get(CLIParam.user), cliArgs.get(CLIParam.password));
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

	protected abstract void run() throws IOException;

	/**
	 * Starts the main processing of the application. It checks the properties
	 * file to see if a user has selected export step or import step. It accordingly
	 * establishes a session with either source repository or target repository.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void start(String docbaseName, String docbaseUser, String passTmp) throws Throwable {
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### CMS Migration Process Started #####");
		}

		final IDfClient dfClient;
		try {
			dfClient = DfClient.getLocalClient();
			if (dfClient == null) {
				// If I don't have a local client then something was not
				// installed
				// correctly so throw an error
				String msg = "No local client was established.  You may want to check the installation of "
					+ "Documentum or this application on this machine.";
				this.logger.error(msg);
				throw new RuntimeException(msg);
			}
		} catch (DfException e) {
			String msg = "No local client was established.  You may want to check the installation of "
				+ "Documentum or this application on this machine.";
			this.logger.error(msg);
			throw new RuntimeException(msg, e);
		}

		if (passTmp != null) {
			try {
				passTmp = RegistryPasswordUtils.decrypt(passTmp);
				if (this.logger.isEnabledFor(Level.INFO)) {
					this.logger.info(String.format("Password decrypted successfully"));
				}
			} catch (Throwable t) {
				// Not encrypted, use literal
				if (this.logger.isEnabledFor(Level.INFO)) {
					this.logger.info(String.format("Password decryption failed, using as literal"));
				}
			}
		}
		final String docbasePassword = passTmp;

		// get a local client
		// Prepare login object
		IDfLoginInfo li = new DfLoginInfo();
		if (docbaseUser != null) {
			li.setUser(docbaseUser);
		}
		if (docbasePassword != null) {
			li.setPassword(docbasePassword);
		}
		li.setDomain(null);

		// Get a documentum session using session manager
		IDfSessionManager sessionManager = dfClient.newSessionManager();
		sessionManager.setIdentity(docbaseName, li);
		this.dctmSession = sessionManager.getSession(docbaseName);

		run();
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.debug("##### CMS Migration Process finished #####");
		}
	}

}