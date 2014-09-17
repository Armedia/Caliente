package com.delta.cmsmf.mainEngine;

import java.io.File;

import org.apache.log4j.Logger;

import com.delta.cmsmf.cfg.Constant;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.cfg.SettingManager;
import com.delta.cmsmf.cms.CmsFileSystem;
import com.delta.cmsmf.cms.DefaultCmsFileSystem;
import com.delta.cmsmf.cms.pool.DctmSessionManager;
import com.delta.cmsmf.cms.storage.CmsObjectStore;
import com.delta.cmsmf.cms.storage.base.DefaultCmsObjectStore;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public abstract class AbstractCMSMFMain implements CMSMFMain {

	/** The log object used for logging. */
	protected final Logger log = Logger.getLogger(getClass());

	private static AbstractCMSMFMain instance = null;

	protected final CmsObjectStore objectStore;
	protected final CmsFileSystem fileSystem;
	protected final DctmSessionManager sessionManager;

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
		this.log.info(String.format("Launching CMSMF %s mode%n", CMSMFLauncher.getParameter(CLIParam.mode)));

		File databaseDirectoryLocation = new File(Setting.DB_DIRECTORY.getString()).getCanonicalFile();
		File contentFilesDirectoryLocation = new File(Setting.CONTENT_DIRECTORY.getString()).getCanonicalFile();

		this.objectStore = DefaultCmsObjectStore.init(requiresCleanData());
		this.fileSystem = new DefaultCmsFileSystem(contentFilesDirectoryLocation);
		this.sessionManager = new DctmSessionManager(CMSMFLauncher.getParameter(CLIParam.docbase),
			CMSMFLauncher.getParameter(CLIParam.user), CMSMFLauncher.getParameter(CLIParam.password));

		// Set the filesystem location where files will be created or read from
		this.log.info(String.format("Using database directory: [%s]", databaseDirectoryLocation));

		// Set the filesystem location where the content files will be created or read from
		this.log.info(String.format("Using content directory: [%s]", contentFilesDirectoryLocation));
	}

	public static AbstractCMSMFMain getInstance() {
		return AbstractCMSMFMain.instance;
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