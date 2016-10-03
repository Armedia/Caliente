package com.delta.cmsmf.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
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

	private static final String STORE_XML_PROPERTIES_BASE = "cmsmf.%s.store.xml";
	private static final String STORE_PROPERTIES_BASE = "cmsmf.%s.store.properties";
	private static final String STORE_TYPE_PROPERTY = "cmsmf.store.type";

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

			Properties storeProps = loadStoreProperties("object", CLIParam.object_store_config.getString());

			StoreConfiguration cfg = CmfStores.getObjectStoreConfiguration("default");
			String storeType = storeProps.getProperty(AbstractCMSMFMain.STORE_TYPE_PROPERTY);
			if (!StringUtils.isEmpty(storeType)) {
				cfg.setType(storeType);
			}
			if (!storeProps.isEmpty()) {
				Map<String, String> m = cfg.getSettings();
				for (String s : storeProps.stringPropertyNames()) {
					String v = storeProps.getProperty(s);
					if (v != null) {
						m.put(s, v);
					}
				}
			}
			cfg.getSettings().put(CmfStoreFactory.CFG_CLEAN_DATA, String.valueOf(clearStorage));
			cfg.getSettings().put("dir.content", contentFilesDirectoryLocation.getAbsolutePath());
			cfg.getSettings().put("dir.metadata", databaseDirectoryLocation.getAbsolutePath());

			this.cmfObjectStore = CmfStores.createObjectStore(cfg);

			final boolean directFsExport = CLIParam.direct_fs.isPresent();

			storeProps = loadStoreProperties("content", CLIParam.content_store_config.getString());
			final String contentStoreName = (directFsExport ? "direct" : "default");
			cfg = CmfStores.getContentStoreConfiguration(contentStoreName);
			if (!directFsExport) {
				String strategy = CLIParam.content_strategy.getString();
				if (StringUtils.isBlank(strategy)) {
					strategy = getContentStrategyName();
				}
				if (!StringUtils.isBlank(strategy)) {
					cfg.getSettings().put("dir.content.strategy", strategy);
				}
				storeType = storeProps.getProperty(AbstractCMSMFMain.STORE_TYPE_PROPERTY);
				if (!StringUtils.isEmpty(storeType)) {
					cfg.setType(storeType);
				}
				if (!storeProps.isEmpty()) {
					Map<String, String> m = cfg.getSettings();
					for (String s : storeProps.stringPropertyNames()) {
						String v = storeProps.getProperty(s);
						if (v != null) {
							m.put(s, v);
						}
					}
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

	private File createFile(String path) {
		File f = new File(path);
		try {
			f = f.getCanonicalFile();
		} catch (IOException e) {
			// Do nothing
		} finally {
			f = f.getAbsoluteFile();
		}
		return f;
	}

	protected Properties loadStoreProperties(String type, final String jdbcConfig) throws IOException {
		Properties p = new Properties();
		if (jdbcConfig != null) {
			File f = createFile(jdbcConfig);
			if (!f.exists()) { throw new IOException(
				String.format("The %s store properties file at [%s] doesn't exist", type, f.getAbsolutePath())); }
			if (!f.isFile()) { throw new IOException(String
				.format("The %s store properties file at [%s] is not a regular file", type, f.getAbsolutePath())); }
			if (!f.canRead()) { throw new IOException(
				String.format("The %s store properties file at [%s] can't be read", type, f.getAbsolutePath())); }

			InputStream in = null;
			boolean ok = false;
			try {
				in = new FileInputStream(f);
				p.loadFromXML(in);
				ok = true;
				return p;
			} catch (InvalidPropertiesFormatException e) {
				this.console.warn("The {} store properties at [{}] aren't in XML format, trying the classic format",
					type, f.getAbsolutePath());
				p.clear();
				IOUtils.closeQuietly(in);
				in = new FileInputStream(f);
				try {
					p.load(in);
				} catch (IllegalArgumentException e2) {
					throw new IOException(
						String.format("Failed to load the %s store properties from [%s]", type, f.getAbsolutePath()),
						e2);
				}
				ok = true;
				return p;
			} finally {
				IOUtils.closeQuietly(in);
				if (ok) {
					this.console.info("Loaded the {} store properties from [{}]", type, f.getAbsolutePath());
				}
			}
		}

		// First, try the XML variant
		File f = createFile(String.format(AbstractCMSMFMain.STORE_XML_PROPERTIES_BASE, type));
		if (f.exists() && f.isFile() && f.canRead()) {
			InputStream in = null;
			try {
				in = new FileInputStream(f);
				p.loadFromXML(in);
				this.console.info("Loaded the {} store XML properties from [{}]", type, f.getAbsolutePath());
				return p;
			} catch (Exception e) {
				throw new IOException(
					String.format("Failed to load the %s store properties XML from [%s]", type, f.getAbsolutePath()),
					e);
			} finally {
				IOUtils.closeQuietly(in);
			}
		}

		f = createFile(String.format(AbstractCMSMFMain.STORE_PROPERTIES_BASE, type));
		if (f.exists() && f.isFile() && f.canRead()) {
			InputStream in = null;
			try {
				in = new FileInputStream(f);
				p.load(in);
				this.console.info("Loaded the {} store properties from [{}]", type, f.getAbsolutePath());
				return p;
			} catch (Exception e) {
				throw new IOException(
					String.format("Failed to load the %s store properties XML from [%s]", type, f.getAbsolutePath()),
					e);
			} finally {
				IOUtils.closeQuietly(in);
			}
		}

		this.console.info("No special {} store properties set, using defaulted values", type);
		return p;
	}
}