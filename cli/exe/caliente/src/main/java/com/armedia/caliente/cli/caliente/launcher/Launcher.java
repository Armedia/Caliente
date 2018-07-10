package com.armedia.caliente.cli.caliente.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.CommandScheme;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.StringValueFilter;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.CalienteCommand;
import com.armedia.caliente.cli.caliente.command.CommandModule;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.cli.exception.CommandLineSyntaxException;
import com.armedia.caliente.cli.exception.DynamicOptionsException;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.CommandLineProcessingException;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.LibLaunchHelper;
import com.armedia.caliente.engine.tools.LocalOrganizationStrategy;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStoreFactory;
import com.armedia.caliente.store.CmfStores;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.Tools;

public class Launcher extends AbstractLauncher {

	public static final void main(String... args) {
		System.exit(new Launcher().launch(CLIParam.help, args));
	}

	private static final String STORE_TYPE_PROPERTY = "caliente.store.type";
	private static final Path DEFAULT_DB_PATH = Paths.get("caliente");
	private static final String DEFAULT_CONTENT_PATH = "content";
	private static final String DEFAULT_CONTENT_STRATEGY = LocalOrganizationStrategy.NAME;

	private final LibLaunchHelper libLaunchHelper = new LibLaunchHelper();

	// Saves us quite a few keystrokes ;)
	private EngineInterface engineInterface = null;

	private CommandModule<?> command = null;

	private File objectStoreLocation = null;
	private CmfObjectStore<?, ?> objectStore = null;

	private File contentStoreLocation = null;
	private CmfContentStore<?, ?, ?> contentStore = null;

	private File logLocation = null;

	private boolean directFsMode = false;

	private String contentStrategy = Launcher.DEFAULT_CONTENT_STRATEGY;

	@Override
	protected String getProgramName() {
		return "caliente";
	}

	@Override
	protected OptionScheme getOptionScheme() {

		final CommandScheme scheme = new CommandScheme(getProgramName(), true);
		for (CalienteCommand d : CalienteCommand.values()) {
			Command c = new Command(true, d.getTitle(), d.getAliases()) {

				@Override
				public void getDynamicOptions(boolean helpRequested, OptionValues baseValues)
					throws CommandLineSyntaxException {
					String err = initializeEngineAndCommand(baseValues, getName());
					if (err != null) {
						if (!helpRequested) { throw new DynamicOptionsException(this, err); }
					}

					if (DynamicOptions.class.isInstance(Launcher.this.engineInterface)) {
						DynamicOptions.class.cast(Launcher.this.engineInterface).getDynamicOptions(this);
					}
					if (DynamicOptions.class.isInstance(Launcher.this.command)) {
						DynamicOptions.class.cast(Launcher.this.command).getDynamicOptions(this);
					}
				}

			};
			c.setDescription(d.getDescription());
			scheme.addCommand(c);
		}

		// Now, find the engines available
		OptionImpl impl = OptionImpl.cast(CLIParam.engine);
		if (impl != null) {
			impl.setValueFilter(new StringValueFilter(false, EngineInterface.getAliases(this.log)));
		}

		return scheme //
			.addGroup(CLIGroup.BASE) //
		;
	}

	private String initializeEngineAndCommand(OptionValues baseValues, String currentCommand) {
		// Has an engine been selected already?
		if (this.engineInterface == null) {
			if (!baseValues.isPresent(
				CLIParam.engine)) { return "No engine was selected in the base options (option order is important!)"; }

			// Find the desired engine
			final String engine = baseValues.getString(CLIParam.engine);
			this.engineInterface = EngineInterface.get(engine);
			if (this.engineInterface == null) { return String.format("No engine was found with the name or alias [%s]",
				engine); }
		}

		// Has a command been given yet?
		if (this.command == null) {
			if (StringUtils.isBlank(currentCommand)) { return "No command was given (option order is important!)"; }

			final CalienteCommand calienteCommand = CalienteCommand.get(currentCommand);
			if (calienteCommand == null) { return String
				.format("Command [%s] is not a valid Caliente command or command alias", currentCommand); }

			this.command = this.engineInterface.getCommandModule(calienteCommand);
			if (this.command == null) { return String.format("Engine [%s] does not support command [%s]",
				this.engineInterface.getName(), calienteCommand.getTitle()); }
		}

		return null;
	}

	@Override
	protected void processCommandLineResult(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws CommandLineProcessingException {

		final String error = initializeEngineAndCommand(baseValues, command);
		if (error != null) { throw new CommandLineProcessingException(1, error); }

		this.objectStoreLocation = getMetadataLocation(baseValues);
		this.contentStoreLocation = getContentLocation(baseValues);
		this.logLocation = getLogLocation(baseValues);

		if ((this.logLocation == null) && (this.objectStoreLocation != null)
			&& (!this.objectStoreLocation.exists() || this.objectStoreLocation.isDirectory())) {
			this.logLocation = this.objectStoreLocation;
		}
		if ((this.logLocation == null) && (this.contentStoreLocation != null)
			&& (!this.contentStoreLocation.exists() || this.contentStoreLocation.isDirectory())) {
			// If there's no object store location, then use the content location as the log
			// location
			this.logLocation = this.contentStoreLocation;
		}

		// If no log location has been selected, use the default
		if (this.logLocation == null) {
			this.logLocation = Tools.canonicalize(new File("."));
		}

		this.directFsMode = baseValues.isPresent(CLIParam.direct_fs);
		this.contentStrategy = baseValues.getString(CLIParam.content_strategy, Launcher.DEFAULT_CONTENT_STRATEGY);
	}

	private File getMetadataLocation(OptionValues baseValues) throws CommandLineProcessingException {
		// Step 1: Does this engine use a special location for metadata?
		File f = Tools.canonicalize(this.command.getMetadataFilesLocation());
		if (f != null) { return f; }

		// Step 2: There is no special location used by the engine, so see what the user wants to do
		String path = null;
		if (baseValues.isPresent(CLIParam.db)) {
			path = baseValues.getString(CLIParam.db);
		} else {
			path = Launcher.DEFAULT_DB_PATH.toString();
		}

		f = createFile(path);
		if (f.exists() && !f.isFile() && !f.isDirectory()) {
			// ERROR! Not a file or directory! What is this?
			throw new CommandLineProcessingException(1, String.format(
				"The object at path [%s] is neither a file nor a directory - can't use it to describe the metadata store",
				f));
		}
		return f;
	}

	private StoreConfiguration configureObjectStore() throws IOException, CommandLineProcessingException {
		final File metadataLocation = this.objectStoreLocation;
		final Map<String, String> commonValues = new HashMap<>();
		if (!metadataLocation.exists() || metadataLocation.isDirectory()) {
			commonValues.put("dir.metadata", metadataLocation.getAbsolutePath());
		}
		commonValues.put("db.name", "caliente");

		StoreConfiguration cfg = CmfStores.getObjectStoreConfiguration("default");
		applyStoreProperties(cfg,
			loadStoreProperties("object", metadataLocation.isFile() ? metadataLocation.getAbsolutePath() : null));

		cfg.getSettings().putAll(commonValues);
		this.command.customizeObjectStoreProperties(cfg);
		commonValues.put(CmfStoreFactory.CFG_CLEAN_DATA,
			String.valueOf(this.command.getDescriptor().isRequiresCleanData()));
		cfg.getSettings().putAll(commonValues);
		return cfg;
	}

	private File getContentLocation(OptionValues baseValues) throws CommandLineProcessingException {
		// Step 1: Does this engine use a special location for content?
		File f = Tools.canonicalize(this.command.getContentFilesLocation());
		if (f != null) { return f; }

		// Step 2: There is no special location used by the engine, so see what the user wants to do
		String path = null;
		if (baseValues.isPresent(CLIParam.content)) {
			path = baseValues.getString(CLIParam.content);
		} else {
			if (this.objectStoreLocation != null) {
				path = new File(this.objectStoreLocation, Launcher.DEFAULT_CONTENT_PATH).getAbsolutePath();
			} else {
				path = Launcher.DEFAULT_DB_PATH.resolve(Launcher.DEFAULT_CONTENT_PATH).toString();
			}
			path = null;
		}

		f = createFile(path);
		if (f.exists() && !f.isFile() && !f.isDirectory()) {
			// ERROR! Not a file or directory! What is this?
			throw new CommandLineProcessingException(1, String.format(
				"The object at path [%s] is neither a file nor a directory - can't use it to describe the content store",
				f));
		}
		return f;
	}

	private StoreConfiguration configureContentStore() throws IOException, CommandLineProcessingException {

		final boolean directFsExport = this.directFsMode;
		final File contentLocation = this.contentStoreLocation;

		Map<String, String> commonValues = new HashMap<>();
		if (!contentLocation.exists() || contentLocation.isDirectory()) {
			commonValues.put("dir.content", contentLocation.getAbsolutePath());
		}
		commonValues.put("db.name", "caliente");

		final String contentStoreName = (directFsExport ? "direct" : "default");
		StoreConfiguration cfg = CmfStores.getContentStoreConfiguration(contentStoreName);
		if (!directFsExport) {
			String strategy = this.contentStrategy;
			if (StringUtils.isBlank(strategy)) {
				strategy = Launcher.DEFAULT_CONTENT_STRATEGY;
			}
			if (!StringUtils.isBlank(strategy)) {
				cfg.getSettings().put("dir.content.strategy", strategy);
			}
			applyStoreProperties(cfg,
				loadStoreProperties("content", contentLocation.isFile() ? contentLocation.getAbsolutePath() : null));
		}
		this.command.customizeContentStoreProperties(cfg);
		if (!directFsExport) {
			String strategy = this.command.getContentStrategyName();
			if (!StringUtils.isBlank(strategy)) {
				cfg.getSettings().put("dir.content.strategy", strategy);
			}
		}
		commonValues.put(CmfStoreFactory.CFG_CLEAN_DATA,
			String.valueOf(this.command.getDescriptor().isRequiresCleanData()));
		cfg.getSettings().putAll(commonValues);
		return cfg;
	}

	private File getLogLocation(OptionValues baseValues) throws CommandLineProcessingException {
		// Step 1: Does this engine use a special location for content?
		if (!baseValues.isPresent(CLIParam.log_dir)) { return null; }
		return Tools.canonicalize(new File(baseValues.getString(CLIParam.log_dir)));
	}

	private void initializeStores() throws Exception {
		if (!this.command.getDescriptor().isRequiresStorage()) { return; }

		CmfStores.initializeConfigurations();

		StoreConfiguration cfg = null;

		cfg = configureObjectStore();
		this.objectStore = CmfStores.createObjectStore(cfg);

		cfg = configureContentStore();
		this.contentStore = CmfStores.createContentStore(cfg);

		// Set the filesystem location where files will be created or read from
		File storeLocation = null;

		storeLocation = this.objectStore.getStoreLocation();
		if (storeLocation != null) {
			this.log.info(String.format("Using database directory: [%s]", storeLocation.getAbsolutePath()));
		}

		// Set the filesystem location where the content files will be created or read from
		storeLocation = this.contentStore.getStoreLocation();
		if (storeLocation != null) {
			this.log.info(String.format("Using content directory: [%s]", storeLocation.getAbsolutePath()));
		}
	}

	protected boolean applyStoreProperties(StoreConfiguration cfg, Properties properties) {
		if ((properties == null) || properties.isEmpty()) { return false; }
		String storeType = properties.getProperty(Launcher.STORE_TYPE_PROPERTY);
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

	private File createFile(String path) {
		return Tools.canonicalize(new File(path));
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

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionValues baseValues, String command,
		OptionValues commandValues, Collection<String> positionals) {
		List<LaunchClasspathHelper> l = new ArrayList<>();
		l.add(this.libLaunchHelper);
		/*
		for (LaunchClasspathHelper h : this.engineFactory.getClasspathHelpers()) {
			l.add(h);
		}
		*/
		return l;
	}

	@Override
	protected boolean initLogging(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) {
		final String engine = this.engineInterface.getName();
		final String logMode = StringUtils.lowerCase(command);
		final String logEngine = StringUtils.lowerCase(engine);
		final String logTimeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		final String logName = baseValues.getString(CLIParam.log);

		// Make sure the log directory always uses forward slashes
		System.setProperty("logDir", this.logLocation.getAbsolutePath().replace('\\', '/'));
		System.setProperty("logName", logName);
		System.setProperty("logTimeStamp", logTimeStamp);
		System.setProperty("logMode", logMode);
		System.setProperty("logEngine", logEngine);

		String logCfg = baseValues.getString(CLIParam.log_cfg);
		boolean customLog = false;
		if (logCfg != null) {
			final File cfg = createFile(logCfg);
			if (cfg.exists() && cfg.isFile() && cfg.canRead()) {
				DOMConfigurator.configure(Tools.canonicalize(cfg).getAbsolutePath());
				customLog = true;
			}
		}

		if (!customLog) {
			// No custom log is in play, so we just use the default one from the classpath
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			URL config = cl.getResource("log4j-full.xml");
			if (config != null) {
				DOMConfigurator.configure(config);
			}
		}

		// Make sure log4j is configured by directly invoking the requisite class
		org.apache.log4j.Logger.getRootLogger().info("Logging active");

		// Now, get the logs via SLF4J, which is what we'll be using moving forward...
		final Logger console = LoggerFactory.getLogger("console");
		console
			.info(String.format("Launching Caliente v%s %s mode for engine %s%n", Caliente.VERSION, command, engine));
		Runtime runtime = Runtime.getRuntime();
		console.info(String.format("Current heap size: %d MB", runtime.totalMemory() / 1024 / 1024));
		console.info(String.format("Maximum heap size: %d MB", runtime.maxMemory() / 1024 / 1024));
		return true;
	}

	@Override
	protected void showBanner(Logger log) {
		log.info("Caliente CLI v{}", Caliente.VERSION);
	}

	@Override
	protected int run(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws Exception {
		try {
			// Now go try to initialize the stores
			initializeStores();
			return new Caliente().run(this.engineInterface.getName(), new CalienteState(this.objectStoreLocation,
				this.objectStore, this.contentStoreLocation, this.contentStore), this.command, commandValues,
				positionals);
		} finally {
			this.command.close();
		}
	}
}