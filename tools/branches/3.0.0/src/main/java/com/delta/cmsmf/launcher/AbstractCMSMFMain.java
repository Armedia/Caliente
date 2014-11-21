package com.delta.cmsmf.launcher;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.TransferEngine;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.Stores;
import com.armedia.cmf.storage.xml.StoreConfiguration;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.cfg.SettingManager;
import com.documentum.fc.common.IDfTime;

public abstract class AbstractCMSMFMain<L, E extends TransferEngine<?, ?, ?, ?, L>> implements CMSMFMain {

	protected static final String JAVA_SQL_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
	protected static final String LAST_EXPORT_DATE_PATTERN = AbstractCMSMFMain.JAVA_SQL_DATETIME_PATTERN;
	protected static final String LAST_EXPORT_DATETIME_PATTERN = IDfTime.DF_TIME_PATTERN26;

	/** The log object used for logging. */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected final Logger console = LoggerFactory.getLogger("console");

	private static AbstractCMSMFMain<?, ?> instance = null;

	protected final ObjectStore<?, ?> objectStore;
	protected final ContentStore contentStore;
	protected final E engine;

	protected final String docbase;
	protected final String user;
	protected final String password;

	AbstractCMSMFMain(E engine) throws Throwable {
		if (engine == null) { throw new IllegalArgumentException("Must provide an engine to operate with"); }

		this.engine = engine;

		// If we have command-line parameters, these supersede all other configurations, even if
		// we have a configuration file explicitly listed.
		this.console.info("Configuring the properties");
		SettingManager.addPropertySource(CMSMFLauncher.getParameterProperties());

		// A configuration file has been specifed, so use its values ahead of the defaults
		if (CLIParam.cfg.getString() != null) {
			SettingManager.addPropertySource(CLIParam.cfg.getString());
		}

		// Now, the catch-all, default configuration
		SettingManager.addPropertySource("cmsmf.properties");

		// And we start up the configuration engine...
		SettingManager.init();
		this.console.info("Properties ready");

		// First things first...
		AbstractCMSMFMain.instance = this;
		this.log.info(String.format("Launching CMSMF %s mode%n", CLIParam.mode.getString()));

		File databaseDirectoryLocation = new File(Setting.DB_DIRECTORY.getString()).getCanonicalFile();
		File contentFilesDirectoryLocation = new File(Setting.CONTENT_DIRECTORY.getString()).getCanonicalFile();

		this.console.info(String.format("Initializing the object store at [%s]", databaseDirectoryLocation));

		StoreConfiguration cfg = Stores.getObjectStoreConfiguration("cmsmf");
		// TODO: Modify the configuration
		this.objectStore = Stores.createObjectStore(cfg);

		cfg = Stores.getContentStoreConfiguration("cmsmf");
		// TODO: Modify the configuration
		this.contentStore = Stores.createContentStore(cfg);

		if (requiresCleanData()) {
			String msg = String.format("Cleaning out the content export directory at [%s]",
				contentFilesDirectoryLocation);
			this.console.info(msg);
			this.log.info(msg);
			this.contentStore.clearAllStreams();

			msg = "Cleaning out all stored metadata";
			this.console.info(msg);
			this.log.info(msg);
			this.objectStore.clearAllObjects();
		}

		this.docbase = CLIParam.docbase.getString();
		this.user = CLIParam.user.getString();
		this.password = CLIParam.password.getString();

		// Set the filesystem location where files will be created or read from
		this.log.info(String.format("Using database directory: [%s]", databaseDirectoryLocation));

		// Set the filesystem location where the content files will be created or read from
		this.log.info(String.format("Using content directory: [%s]", contentFilesDirectoryLocation));
	}

	public static AbstractCMSMFMain<?, ?> getInstance() {
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