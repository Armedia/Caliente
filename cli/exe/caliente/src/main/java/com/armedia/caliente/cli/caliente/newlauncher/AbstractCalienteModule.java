package com.armedia.caliente.cli.caliente.newlauncher;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.cfg.SettingManager;
import com.armedia.caliente.cli.caliente.launcher.CalienteMain;
import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.engine.tools.LocalOrganizationStrategy;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.caliente.tools.CmfCrypt;

public abstract class AbstractCalienteModule<L, E extends TransferEngine<?, ?, ?, ?, ?, L>> implements CalienteMain {

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
}