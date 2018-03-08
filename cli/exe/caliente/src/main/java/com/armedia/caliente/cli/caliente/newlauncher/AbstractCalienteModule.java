package com.armedia.caliente.cli.caliente.newlauncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.cfg.SettingManager;
import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.engine.tools.LocalOrganizationStrategy;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStoreFactory;
import com.armedia.caliente.store.CmfStores;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.xml.XmlProperties;

public abstract class AbstractCalienteModule<L, E extends TransferEngine<?, ?, ?, ?, ?, L>> implements CalienteMain {

	private static final String LEGACY_DB = "cmsmf-data";
	private static final String CURRENT_DB = "caliente";

	private static final String STORE_TYPE_PROPERTY = "caliente.store.type";

	private static final String DEFAULT_SETTINGS_NAME = "caliente.properties";

	protected static final int DEFAULT_THREADS = (Runtime.getRuntime().availableProcessors() * 2);

	protected static final String ALL = "ALL";

	protected static final String JAVA_SQL_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
	protected static final String LAST_EXPORT_DATE_PATTERN = AbstractCalienteModule.JAVA_SQL_DATETIME_PATTERN;

	/** The log object used for logging. */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected final Logger console = LoggerFactory.getLogger("console");
	protected final CalienteWarningTracker warningTracker;

	private static AbstractCalienteModule<?, ?> instance = null;

	protected final CmfObjectStore<?, ?> cmfObjectStore;
	protected final CmfContentStore<?, ?, ?> cmfContentStore;
	protected final E engine;

	protected final String server;
	protected final String user;
	protected final String password;
	protected final String domain;

	private static boolean isLegacyMode(File dbDir) {
		File legacyDb = new File(dbDir, String.format("%s.mv.db", AbstractCalienteModule.LEGACY_DB));
		return (legacyDb.exists() && legacyDb.isFile() && legacyDb.canRead() && legacyDb.canWrite());
	}

	protected AbstractCalienteModule(E engine, boolean requiresStorage, boolean clearMetadata, boolean clearContent)
		throws Throwable {
		if (engine == null) { throw new IllegalArgumentException("Must provide an engine to operate with"); }
		this.engine = engine;

		// If we have command-line parameters, these supersede all other configurations, even if
		// we have a configuration file explicitly listed.
		this.console.info(String.format("Caliente CLI v%s", Caliente.VERSION));
		this.console.info("Configuring the properties");

		// And we start up the configuration engine...
		SettingManager.init();
		this.console.info("Properties ready");

		// First things first...
		AbstractCalienteModule.instance = this;

		if (requiresStorage) {
			final File databaseDirectoryLocation = getMetadataFilesLocation().getCanonicalFile();
			// Identify whether to use legacy mode or not...
			final boolean legacyMode = AbstractCalienteModule.isLegacyMode(databaseDirectoryLocation);
			final String dbName = (legacyMode ? AbstractCalienteModule.LEGACY_DB : AbstractCalienteModule.CURRENT_DB);
			final File contentFilesDirectoryLocation = getContentFilesLocation().getCanonicalFile();

			this.console.info(String.format("Initializing the object store at [%s]", databaseDirectoryLocation));

			Map<String, String> commonValues = new HashMap<>();
			commonValues.put("dir.content", contentFilesDirectoryLocation.getAbsolutePath());
			commonValues.put("dir.metadata", databaseDirectoryLocation.getAbsolutePath());
			commonValues.put("db.name", dbName);
			commonValues.put("legacyMode", String.valueOf(legacyMode));

			CmfStores.initializeConfigurations();

			StoreConfiguration cfg = CmfStores.getObjectStoreConfiguration("default");
			customizeObjectStoreProperties(cfg);
			applyStoreProperties(cfg, loadStoreProperties("object", CLIParam.object_store_config.getString()));
			commonValues.put(CmfStoreFactory.CFG_CLEAN_DATA, String.valueOf(clearMetadata));
			cfg.getSettings().putAll(commonValues);
			this.cmfObjectStore = CmfStores.createObjectStore(cfg);

			final boolean directFsExport = CLIParam.direct_fs.isPresent();

			final String contentStoreName = (directFsExport ? "direct" : "default");
			cfg = CmfStores.getContentStoreConfiguration(contentStoreName);
			customizeContentStoreProperties(cfg);
			if (!directFsExport) {
				String strategy = CLIParam.content_strategy.getString();
				if (StringUtils.isBlank(strategy)) {
					strategy = getContentStrategyName();
				}
				if (!StringUtils.isBlank(strategy)) {
					cfg.getSettings().put("dir.content.strategy", strategy);
				}
				applyStoreProperties(cfg, loadStoreProperties("content", CLIParam.content_store_config.getString()));
			}
			commonValues.put(CmfStoreFactory.CFG_CLEAN_DATA, String.valueOf(clearContent));
			cfg.getSettings().putAll(commonValues);
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
		this.warningTracker = new CalienteWarningTracker(this.console, true);
	}

	protected void customizeObjectStoreProperties(StoreConfiguration cfg) {
		// Do nothing by default
	}

	protected void customizeContentStoreProperties(StoreConfiguration cfg) {
		// Do nothing by default
	}

	protected File getMetadataFilesLocation() {
		return new File(Setting.DB_DIRECTORY.getString());
	}

	protected File getContentFilesLocation() {
		return new File(Setting.CONTENT_DIRECTORY.getString());
	}

	public static AbstractCalienteModule<?, ?> getInstance() {
		return AbstractCalienteModule.instance;
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

	protected File locateFile(String path, boolean required) throws IOException {
		File f = createFile(path);
		if (!f.exists()) {
			if (required) { throw new IOException(String.format("The file [%s] doesn't exist", f.getAbsolutePath())); }
			return null;
		}

		// We've found the path we're looking for...verify that it's regular file. Otherwise,
		// just ignore it. If this is an explicit configuration setting, then we explode!
		if (!f.isFile()) {
			if (required) { throw new IOException(
				String.format("The file [%s] is not a regular file", f.getAbsolutePath())); }
			return null;
		}

		// Regardless, if it exists and is a regular file, explode if we can't read it
		if (!f.canRead()) { throw new IOException(String.format("The file [%s] can't be read", f.getAbsolutePath())); }

		return f;
	}

	protected Properties loadStoreProperties(String type, String jdbcConfig) throws IOException {
		final boolean usingDefault = (jdbcConfig == null);
		boolean loadedSettingsFile = false;

		if (usingDefault) {
			// Follow a default value if it exists
			jdbcConfig = String.format("caliente-%s-store.properties", type.toLowerCase());
		}

		// Try to find the file...only explode if it's been explicitly requested
		File f = locateFile(jdbcConfig, !usingDefault);
		if ((f == null) && usingDefault) {
			jdbcConfig = String.format("caliente-%s-store.xml", type.toLowerCase());
			f = locateFile(jdbcConfig, false);
		}

		if (f == null) {
			this.console.info("No special {} store properties set, using defaulted values", type);
			return new Properties();
		}

		final boolean supportsFallback = (!StringUtils.endsWithIgnoreCase(jdbcConfig, ".xml"));

		try {
			// Ok...so we have the file...try to load it!
			try (InputStream xmlIn = new FileInputStream(f)) {
				Properties p = XmlProperties.loadFromXML(xmlIn);
				loadedSettingsFile = true;
				return p;
			} catch (InvalidPropertiesFormatException | XMLStreamException e) {
				if (this.log.isTraceEnabled()) {
					this.log.trace("The {} store properties at [{}] aren't in XML format{}", type, f.getAbsolutePath(),
						supportsFallback ? ", trying the classic format" : "", e);
				}
				this.console.warn("The {} store properties at [{}] aren't in XML format", type, f.getAbsolutePath(),
					supportsFallback ? ", trying the classic format" : "");
			}

			if (supportsFallback) {
				// We only make it this far if the XML read failed, and the file isn't named "*.xml"
				try (InputStream textIn = new FileInputStream(f)) {
					Properties p = new Properties();
					p.load(textIn);
					loadedSettingsFile = true;
					return p;
				} catch (IllegalArgumentException e) {
					if (this.log.isTraceEnabled()) {
						this.log.trace("The {} store properties at [{}] aren't in the legacy format", type,
							f.getAbsolutePath(), e);
					}
					this.console.warn("The {} store properties at [{}] aren't in the legacy format", type,
						f.getAbsolutePath());
				}
			}
		} finally {
			if (loadedSettingsFile) {
				this.console.info("Loaded the {} store properties from [{}]", type, f.getAbsolutePath());
			}
		}
		throw new IOException(String.format(
			"Failed to load the %s store properties from [%s] - the file is not a properties file (XML or legacy)",
			type, f.getAbsolutePath()));
	}

	protected boolean applyStoreProperties(StoreConfiguration cfg, Properties properties) {
		if ((properties == null) || properties.isEmpty()) { return false; }
		String storeType = properties.getProperty(AbstractCalienteModule.STORE_TYPE_PROPERTY);
		if (!StringUtils.isEmpty(storeType)) {
			cfg.setType(storeType);
		}
		Map<String, String> m = cfg.getSettings();
		for (String s : properties.stringPropertyNames()) {
			String v = properties.getProperty(s);
			if (v != null) {
				m.put(s, v);
			}
		}
		return true;
	}
}