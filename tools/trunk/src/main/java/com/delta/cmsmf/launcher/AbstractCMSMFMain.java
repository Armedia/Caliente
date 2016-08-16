package com.delta.cmsmf.launcher;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.CmfCrypt;
import com.armedia.cmf.engine.TransferEngine;
import com.armedia.cmf.engine.tools.LocalOrganizationStrategy;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfStoreFactory;
import com.armedia.cmf.storage.CmfStores;
import com.armedia.cmf.storage.xml.StoreConfiguration;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.cfg.SettingManager;

public abstract class AbstractCMSMFMain<L, E extends TransferEngine<?, ?, ?, ?, ?, L>> implements CMSMFMain {

	protected static final int DEFAULT_THREADS = (Runtime.getRuntime().availableProcessors() * 2);

	protected static final String ALL = "ALL";

	protected static final String JAVA_SQL_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
	protected static final String LAST_EXPORT_DATE_PATTERN = AbstractCMSMFMain.JAVA_SQL_DATETIME_PATTERN;

	/** The log object used for logging. */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected final Logger console = LoggerFactory.getLogger("console");

	private static AbstractCMSMFMain<?, ?> instance = null;

	protected final CmfObjectStore<?, ?> cmfObjectStore;
	protected final CmfContentStore<?, ?, ?> cmfContentStore;
	protected final E engine;

	protected final String server;
	protected final String user;
	protected final String password;
	protected final String domain;

	protected AbstractCMSMFMain(E engine, boolean requiresStorage, boolean clearStorage) throws Throwable {
		if (engine == null) { throw new IllegalArgumentException("Must provide an engine to operate with"); }
		this.engine = engine;

		// If we have command-line parameters, these supersede all other configurations, even if
		// we have a configuration file explicitly listed.
		this.console.info(String.format("CMSMF v%s", CMSMFLauncher.VERSION));
		this.console.info("Configuring the properties");

		// The catch-all, default configuration
		SettingManager.addPropertySource("cmsmf.properties");
		// A configuration file has been specified, so use its values ahead of the defaults
		if (CLIParam.cfg.getString() != null) {
			SettingManager.addPropertySource(CLIParam.cfg.getString());
		}
		SettingManager.addPropertySource(CMSMFLauncher.getParameterProperties());

		// And we start up the configuration engine...
		SettingManager.init();
		this.console.info("Properties ready");

		// First things first...
		AbstractCMSMFMain.instance = this;

		if (requiresStorage) {
			File databaseDirectoryLocation = new File(Setting.DB_DIRECTORY.getString()).getCanonicalFile();
			File contentFilesDirectoryLocation = new File(Setting.CONTENT_DIRECTORY.getString()).getCanonicalFile();

			this.console.info(String.format("Initializing the object store at [%s]", databaseDirectoryLocation));

			CmfStores.initializeConfigurations();

			// TODO: Add support for configurable store names
			StoreConfiguration cfg = CmfStores.getObjectStoreConfiguration("jdbc");
			cfg.getSettings().put(CmfStoreFactory.CFG_CLEAN_DATA, String.valueOf(clearStorage));
			cfg.getSettings().put("dir.content", contentFilesDirectoryLocation.getAbsolutePath());
			cfg.getSettings().put("dir.metadata", databaseDirectoryLocation.getAbsolutePath());
			this.cmfObjectStore = CmfStores.createObjectStore(cfg);

			final boolean directFsExport = CLIParam.direct_fs.isPresent();

			// TODO: Add support for JDBC content storage
			final String contentStoreName = (directFsExport ? "direct" : "local");
			cfg = CmfStores.getContentStoreConfiguration(contentStoreName);
			if (!directFsExport) {
				String strategy = CLIParam.content_strategy.getString();
				if (StringUtils.isBlank(strategy)) {
					strategy = getContentStrategyName();
				}
				if (!StringUtils.isBlank(strategy)) {
					cfg.getSettings().put("dir.content.strategy", strategy);
				}
			}
			cfg.getSettings().put(CmfStoreFactory.CFG_CLEAN_DATA, String.valueOf(clearStorage));
			cfg.getSettings().put("dir.content", contentFilesDirectoryLocation.getAbsolutePath());
			cfg.getSettings().put("dir.metadata", databaseDirectoryLocation.getAbsolutePath());

			this.cmfContentStore = CmfStores.createContentStore(cfg);

			// Set the filesystem location where files will be created or read from
			this.log.info(String.format("Using database directory: [%s]", databaseDirectoryLocation));

			// Set the filesystem location where the content files will be created or read from
			this.log.info(String.format("Using content directory: [%s]", contentFilesDirectoryLocation));
		} else {
			this.cmfObjectStore = null;
			this.cmfContentStore = null;
		}

		this.server = CLIParam.server.getString();
		this.user = CLIParam.user.getString();
		String pass = CLIParam.password.getString();
		CmfCrypt crypto = this.engine.getCrypto();
		this.password = (pass != null ? crypto.encrypt(crypto.decrypt(pass)) : null);
		this.domain = CLIParam.domain.getString();
	}

	public static AbstractCMSMFMain<?, ?> getInstance() {
		return AbstractCMSMFMain.instance;
	}

	@Override
	public CmfObjectStore<?, ?> getObjectStore() {
		return this.cmfObjectStore;
	}

	protected String getContentStrategyName() {
		return LocalOrganizationStrategy.NAME;
	}
}