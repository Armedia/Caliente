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
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.CommandScheme;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionSchemeExtender;
import com.armedia.caliente.cli.OptionSchemeExtensionSupport;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.StringValueFilter;
import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.CalienteBaseOptions;
import com.armedia.caliente.cli.caliente.command.CalienteCommand;
import com.armedia.caliente.cli.caliente.command.CommandModule;
import com.armedia.caliente.cli.exception.CommandLineExtensionException;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.CommandLineProcessingException;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.cli.utils.LibLaunchHelper;
import com.armedia.caliente.engine.tools.LocalOrganizationStrategy;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStoreFactory;
import com.armedia.caliente.store.CmfStores;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.Tools;

public class Launcher extends AbstractLauncher implements OptionSchemeExtensionSupport {

	public static final void main(String... args) {
		System.exit(new Launcher().launch(CalienteBaseOptions.HELP, args));
	}

	public static final String DEFAULT_LOG_FORMAT = CalienteBaseOptions.DEFAULT_LOG_FORMAT;

	private static final String STORE_TYPE_PROPERTY = "caliente.store.type";
	private static final Path DEFAULT_DB_PATH = Paths.get("caliente");
	private static final String DEFAULT_CONTENT_PATH = "content";
	private static final String DEFAULT_CONTENT_STRATEGY = LocalOrganizationStrategy.NAME;

	private final LibLaunchHelper libLaunchHelper = new LibLaunchHelper();

	// Saves us quite a few keystrokes ;)
	private EngineInterface engineInterface = null;

	private CommandModule<?> command = null;

	private CmfObjectStore<?, ?> objectStore = null;

	private CmfContentStore<?, ?, ?> contentStore = null;

	@Override
	protected String getProgramName() {
		return "caliente";
	}

	@Override
	protected OptionScheme getOptionScheme() {

		CommandScheme scheme = new CommandScheme(getProgramName(), true);
		for (CalienteCommand d : CalienteCommand.values()) {
			Command c = new Command(d.getTitle(), d.getAliases());
			c.setDescription(d.getDescription());
			scheme.addCommand(c);
		}

		// Now, find the engines available
		OptionImpl impl = OptionImpl.cast(CalienteBaseOptions.ENGINE);
		if (impl != null) {
			impl.setValueFilter(new StringValueFilter(false, EngineInterface.getAliases(this.log)));
		}

		return scheme.add( //
			new CalienteBaseOptions().asGroup() //
		);
	}

	@Override
	protected OptionSchemeExtensionSupport getSchemeExtensionSupport() {
		return this;
	}

	@Override
	public void extendScheme(int currentNumber, OptionValues baseValues, String currentCommand,
		OptionValues commandValues, Token currentToken, OptionSchemeExtender extender)
		throws CommandLineExtensionException {

		// Has an engine been selected already?
		if (this.engineInterface == null) {
			if (!baseValues.isPresent(CalienteBaseOptions.ENGINE)) { throw new CommandLineExtensionException(
				currentNumber, baseValues, currentCommand, commandValues, currentToken,
				"No engine has been selected in the base options yet (option order is important!)"); }

			// Find the desired engine
			final String engine = baseValues.getString(CalienteBaseOptions.ENGINE);
			this.engineInterface = EngineInterface.get(engine);
			if (this.engineInterface == null) { throw new CommandLineExtensionException(currentNumber, baseValues,
				currentCommand, commandValues, currentToken,
				String.format("No engine was found with the title or alias [%s]", engine)); }
		}

		// Has a command been given yet?
		if (this.command == null) {
			if (StringUtils.isBlank(
				currentCommand)) { throw new CommandLineExtensionException(currentNumber, baseValues, currentCommand,
					commandValues, currentToken, "No command has been given yet (option order is important!)"); }

			final CalienteCommand calienteCommand = CalienteCommand.get(currentCommand);
			if (calienteCommand == null) { throw new CommandLineExtensionException(currentNumber, baseValues,
				currentCommand, commandValues, currentToken,
				String.format("Command [%s] is not a valid Caliente command or command alias", currentCommand)); }

			this.command = this.engineInterface.getCommandModule(calienteCommand);
			if (this.command == null) { throw new CommandLineExtensionException(currentNumber, baseValues,
				currentCommand, commandValues, currentToken, String.format("Engine [%s] does not support command [%s]",
					this.engineInterface.getName(), calienteCommand.getTitle())); }
		}

		// Extend the command lines as per the engine and command
		if (OptionSchemeExtensionSupport.class.isInstance(this.engineInterface)) {
			OptionSchemeExtensionSupport.class.cast(this.engineInterface).extendScheme(currentNumber, baseValues,
				currentCommand, commandValues, currentToken, extender);
		}
		if (OptionSchemeExtensionSupport.class.isInstance(this.command)) {
			OptionSchemeExtensionSupport.class.cast(this.command).extendScheme(currentNumber, baseValues,
				currentCommand, commandValues, currentToken, extender);
		}
	}

	@Override
	protected void processCommandLineResult(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws CommandLineProcessingException {

		// Validate all parameters...make sure everything is kosher, etc...
		if (this.command == null) { throw new CommandLineProcessingException(1,
			String.format("No command was given. The command must be one of %s (case-insensitive)",
				CalienteCommand.getAllAliases())); }

		// Now go try to initialize the stores if required
		try {
			initializeStores(baseValues);
		} catch (Exception e) {
			throw new CommandLineProcessingException(1, "Failed to initialize the metadata/content stores", e);
		}
	}

	private File getMetadataLocation(OptionValues baseValues) throws CommandLineProcessingException {
		// Step 1: Does this engine use a special location for metadata?
		File f = Tools.canonicalize(this.command.getMetadataFilesLocation());
		if (f != null) { return f; }

		// Step 2: There is no special location used by the engine, so see what the user wants to do
		String path = baseValues.getString(CalienteBaseOptions.DB);
		f = createFile(path);
		if (f.exists() && !f.isFile() && !f.isDirectory()) {
			// ERROR! Not a file or directory! What is this?
			throw new CommandLineProcessingException(1, String.format(
				"The object at path [%s] is neither a file nor a directory - can't use it to describe the metadata store",
				f));
		}
		return f;
	}

	private StoreConfiguration configureObjectStore(OptionValues baseValues)
		throws IOException, CommandLineProcessingException {
		final File metadataLocation = getMetadataLocation(baseValues);
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

	private File getContentLocation(OptionValues baseValues, File metadataLocation)
		throws CommandLineProcessingException {
		// Step 1: Does this engine use a special location for content?
		File f = Tools.canonicalize(this.command.getContentFilesLocation());
		if (f != null) { return f; }

		// Step 2: There is no special location used by the engine, so see what the user wants to do
		String path = null;
		if (baseValues.isPresent(CalienteBaseOptions.CONTENT)) {
			path = baseValues.getString(CalienteBaseOptions.CONTENT);
		} else {
			if (metadataLocation != null) {
				path = new File(metadataLocation, Launcher.DEFAULT_CONTENT_PATH).getAbsolutePath();
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

	private StoreConfiguration configureContentStore(OptionValues baseValues)
		throws IOException, CommandLineProcessingException {

		final boolean directFsExport = baseValues.isPresent(CLIParam.direct_fs);
		final File contentLocation = getContentLocation(baseValues, this.objectStore.getStoreLocation());

		Map<String, String> commonValues = new HashMap<>();
		if (!contentLocation.exists() || contentLocation.isDirectory()) {
			commonValues.put("dir.content", contentLocation.getAbsolutePath());
		}
		commonValues.put("db.name", "caliente");

		final String contentStoreName = (directFsExport ? "direct" : "default");
		StoreConfiguration cfg = CmfStores.getContentStoreConfiguration(contentStoreName);
		if (!directFsExport) {
			String strategy = baseValues.getString(CLIParam.content_strategy);
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

	private void initializeStores(OptionValues baseValues) throws Exception {
		if (!this.command.getDescriptor().isRequiresStorage()) { return; }

		CmfStores.initializeConfigurations();

		StoreConfiguration cfg = null;

		cfg = configureObjectStore(baseValues);
		this.objectStore = CmfStores.createObjectStore(cfg);

		cfg = configureContentStore(baseValues);
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
		final String logName = baseValues.getString(CalienteBaseOptions.LOG);

		// TODO: Write the log out into the DB directory
		final File logDir = Tools.canonicalize(new File("."));

		// Make sure the log directory always uses forward slashes
		System.setProperty("logDir", logDir.getAbsolutePath().replace('\\', '/'));
		System.setProperty("logName", logName);
		System.setProperty("logTimeStamp", logTimeStamp);
		System.setProperty("logMode", logMode);
		System.setProperty("logEngine", logEngine);

		String logCfg = baseValues.getString(CalienteBaseOptions.LOG_CFG);
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
			URL config = cl.getResource("log4j.xml");
			if (config != null) {
				DOMConfigurator.configure(config);
			} else {
				config = cl.getResource("log4j.properties");
				if (config != null) {
					PropertyConfigurator.configure(config);
				}
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
			return new Caliente().run(this.engineInterface.getName(), this.objectStore, this.contentStore, this.command,
				commandValues, positionals);
		} finally {
			this.command.close();
		}
	}
}