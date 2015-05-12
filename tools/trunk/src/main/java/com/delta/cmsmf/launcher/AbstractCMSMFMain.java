package com.delta.cmsmf.launcher;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.TransferEngine;
import com.armedia.cmf.engine.tools.LocalURIStrategy;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoreFactory;
import com.armedia.cmf.storage.Stores;
import com.armedia.cmf.storage.xml.StoreConfiguration;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.cfg.SettingManager;

public abstract class AbstractCMSMFMain<L, E extends TransferEngine<?, ?, ?, ?, ?, L>> implements CMSMFMain {

	protected static final String ALL = "ALL";

	protected static final String JAVA_SQL_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
	protected static final String LAST_EXPORT_DATE_PATTERN = AbstractCMSMFMain.JAVA_SQL_DATETIME_PATTERN;

	/** The log object used for logging. */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected final Logger console = LoggerFactory.getLogger("console");

	private static AbstractCMSMFMain<?, ?> instance = null;

	protected final ObjectStore<?, ?> objectStore;
	protected final ContentStore contentStore;
	protected final E engine;

	protected final String server;
	protected final String user;
	protected final String password;
	protected final String domain;

	protected AbstractCMSMFMain(E engine) throws Throwable {
		if (engine == null) { throw new IllegalArgumentException("Must provide an engine to operate with"); }

		this.engine = engine;

		// If we have command-line parameters, these supersede all other configurations, even if
		// we have a configuration file explicitly listed.
		this.console.info(String.format("CMSMF v%s", CMSMFLauncher.VERSION));
		this.console.info("Configuring the properties");
		SettingManager.addPropertySource(CMSMFLauncher.getParameterProperties());

		// A configuration file has been specified, so use its values ahead of the defaults
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

		File databaseDirectoryLocation = new File(Setting.DB_DIRECTORY.getString()).getCanonicalFile();
		File contentFilesDirectoryLocation = new File(Setting.CONTENT_DIRECTORY.getString()).getCanonicalFile();

		this.console.info(String.format("Initializing the object store at [%s]", databaseDirectoryLocation));

		Stores.initializeConfigurations();

		StoreConfiguration cfg = Stores.getObjectStoreConfiguration("cmsmf");
		cfg.getSettings().put(StoreFactory.CFG_CLEAN_DATA, String.valueOf(requiresCleanData()));
		cfg.getSettings().put("dir.metadata", databaseDirectoryLocation.getAbsolutePath());
		this.objectStore = Stores.createObjectStore(cfg);

		cfg = Stores.getContentStoreConfiguration("cmsmf");
		cfg.getSettings().put("dir.content", contentFilesDirectoryLocation.getAbsolutePath());
		cfg.getSettings().put(StoreFactory.CFG_CLEAN_DATA, String.valueOf(requiresCleanData()));

		String strategy = getContentStrategyName();
		if (strategy != null) {
			cfg.getSettings().put("dir.content.strategy", strategy);
		}
		this.contentStore = Stores.createContentStore(cfg);

		this.server = CLIParam.server.getString();
		this.user = CLIParam.user.getString();
		this.password = CLIParam.password.getString();
		this.domain = CLIParam.domain.getString();

		// Set the filesystem location where files will be created or read from
		this.log.info(String.format("Using database directory: [%s]", databaseDirectoryLocation));

		// Set the filesystem location where the content files will be created or read from
		this.log.info(String.format("Using content directory: [%s]", contentFilesDirectoryLocation));
	}

	public static AbstractCMSMFMain<?, ?> getInstance() {
		return AbstractCMSMFMain.instance;
	}

	@Override
	public ObjectStore<?, ?> getObjectStore() {
		return this.objectStore;
	}

	@Override
	public boolean requiresDataStore() {
		return true;
	}

	@Override
	public boolean requiresCleanData() {
		return false;
	}

	protected String getContentStrategyName() {
		return LocalURIStrategy.NAME;
	}
}