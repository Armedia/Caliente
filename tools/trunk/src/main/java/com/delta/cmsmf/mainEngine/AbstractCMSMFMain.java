package com.delta.cmsmf.mainEngine;

import java.io.File;

import org.apache.log4j.Logger;

import com.delta.cmsmf.cfg.Constant;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.cfg.SettingManager;
import com.delta.cmsmf.datastore.DataStore;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public abstract class AbstractCMSMFMain implements CMSMFMain {

	/** The logger object used for logging. */
	protected final Logger logger = Logger.getLogger(getClass());

	private static AbstractCMSMFMain instance = null;

	/** The directory location where stream files will be created. */
	protected final File streamFilesDirectoryLocation;

	/** The directory location where content files will be created. */
	protected final File contentFilesDirectoryLocation;

	protected final boolean testMode;

	AbstractCMSMFMain() throws Throwable {

		// If we have command-line parameters, these supersede all other configurations, even if
		// we have a configuration file explicitly listed.
		SettingManager.addPropertySource(CMSMFLauncher.getParameterProperties());

		// A configuration file has been specifed, so use its values ahead of the defaults
		if (CMSMFLauncher.getParameter(CLIParam.cfg) != null) {
			SettingManager.addPropertySource(CMSMFLauncher.getParameter(CLIParam.cfg));
		}

		// Now, the catch-all, default configuration
		SettingManager.addPropertySource(Constant.FULLY_QUALIFIED_CONFIG_FILE_NAME);

		// And we start up the configuration engine...
		SettingManager.init();

		// First things first...
		AbstractCMSMFMain.instance = this;
		this.logger.info(String.format("Launching CMSMF %s mode%n", CMSMFLauncher.getParameter(CLIParam.mode)));

		// Intiialize the data store
		DataStore.init(requiresCleanData());

		this.testMode = (CMSMFLauncher.getParameter(CLIParam.test) != null);

		// Set the filesystem location where files will be created or read from
		this.streamFilesDirectoryLocation = new File(Setting.STREAMS_DIRECTORY.getString()).getCanonicalFile();
		this.logger.info(String.format("Using streams directory: [%s]", this.contentFilesDirectoryLocation));

		// Set the filesystem location where the content files will be created or read from
		this.contentFilesDirectoryLocation = new File(Setting.CONTENT_DIRECTORY.getString()).getCanonicalFile();
		this.logger.info(String.format("Using content directory: [%s]", this.contentFilesDirectoryLocation));
	}

	public static AbstractCMSMFMain getInstance() {
		return AbstractCMSMFMain.instance;
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

	@Override
	public boolean requiresDataStore() {
		return true;
	}

	@Override
	public boolean requiresCleanData() {
		return false;
	}
}