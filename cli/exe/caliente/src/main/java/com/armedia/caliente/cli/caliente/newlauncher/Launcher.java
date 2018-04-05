package com.armedia.caliente.cli.caliente.newlauncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.CommandScheme;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionSchemeExtensionSupport;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.CalienteBaseOptions;
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
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.PluggableServiceSelector;
import com.armedia.commons.utilities.Tools;

public class Launcher extends AbstractLauncher {

	public static final void main(String... args) {
		System.exit(new Launcher().launch(CLIParam.help, args));
	}

	@Override
	protected OptionSchemeExtensionSupport getSchemeExtensionSupport() {
		return new CalienteOptionSchemeExtension();
	}

	public static final String DEFAULT_LOG_FORMAT = CalienteBaseOptions.DEFAULT_LOG_FORMAT;

	private static final String STORE_TYPE_PROPERTY = "caliente.store.type";
	private static final String DEFAULT_DB_PATH = Paths.get("caliente").toString();
	private static final String DEFAULT_CONTENT_PATH = Paths.get(Launcher.DEFAULT_DB_PATH, "content").toString();
	private static final String DEFAULT_CONTENT_STRATEGY = LocalOrganizationStrategy.NAME;

	private final LibLaunchHelper libLaunchHelper = new LibLaunchHelper();

	// Saves us quite a few keystrokes ;)
	@SuppressWarnings("rawtypes")
	private EngineFactory engineFactory = null;

	private CommandModule command = null;

	private CmfObjectStore<?, ?> objectStore = null;

	private CmfContentStore<?, ?, ?> contentStore = null;

	private final Map<String, CommandFactory> commandFactories = new TreeMap<>();

	@Override
	protected String getProgramName() {
		return "caliente";
	}

	@Override
	protected OptionScheme getOptionScheme() {
		PluggableServiceLocator<CommandFactory> commandFactories = new PluggableServiceLocator<>(CommandFactory.class);
		commandFactories.setErrorListener(new PluggableServiceLocator.ErrorListener() {
			@Override
			public void errorRaised(Class<?> serviceClass, Throwable t) {
				Launcher.this.log.error("Failed to initialize the CommandFactory class {}",
					serviceClass.getCanonicalName(), t);
			}
		});
		commandFactories.setHideErrors(false);

		Map<String, CommandDescriptor> commands = new TreeMap<>();
		for (CommandFactory f : commandFactories) {
			for (CommandDescriptor d : f) {
				CommandDescriptor od = commands.put(d.getName(), d);
				if (od != null) {
					// ERROR!
					throw new RuntimeException(String.format(
						"Duplicate command definition for [%s] (new aliases = %s, old aliases = %s) - this is a code defect",
						d.getName(), d.getAliases(), od.getAliases()));
				}
				this.commandFactories.put(d.getName().toLowerCase(), f);

				for (String alias : d.getAliases()) {
					od = commands.put(alias, d);
					if ((od != null) && (od != d)) {
						// ERROR!
						throw new RuntimeException(String.format(
							"Command alias [%s] has a collision between commands [%s] and [%s] - this is a code defect",
							alias, d.getName(), od.getName()));
					}
					this.commandFactories.put(alias.toLowerCase(), f);
				}
			}
		}

		CommandScheme scheme = new CommandScheme(getProgramName(), true);
		for (CommandDescriptor d : commands.values()) {
			Command c = new Command(d.getName(), d.getAliases());
			c.setDescription(d.getDescription());
			scheme.addCommand(c);
		}

		return scheme.add( //
			new OptionGroupImpl("Base Options") //
				.add(CLIParam.lib) //
				.add(CLIParam.log) //
				.add(CLIParam.log_cfg) //
				.add(CLIParam.engine) //
				.add(CLIParam.db) //
				.add(CLIParam.content) //
		);
	}

	@Override
	protected void processCommandLineResult(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws CommandLineProcessingException {

		// Validate all parameters...make sure everything is kosher, etc...

		// Find the desired command
		CommandFactory commandFactory = this.commandFactories.get(StringUtils.lowerCase(command));
		if (commandFactory == null) { throw new CommandLineProcessingException(1,
			String.format("No implementation found for command or alias [%s]", command)); }

		this.command = commandFactory.getCommand(command, commandValues, positionals);

		// Find the desired engine, and add its classpath helpers if required
		final String engine = CLIParam.engine.getString(baseValues);

		@SuppressWarnings("rawtypes")
		final PluggableServiceLocator<EngineFactory> engineFactories = new PluggableServiceLocator<>(
			EngineFactory.class);
		engineFactories.setErrorListener(new PluggableServiceLocator.ErrorListener() {
			@Override
			public void errorRaised(Class<?> serviceClass, Throwable t) {
				Launcher.this.log.error("Failed to initialize the EngineFactory class {}",
					serviceClass.getCanonicalName(), t);
			}
		});

		@SuppressWarnings("rawtypes")
		final PluggableServiceSelector<EngineFactory> selector = new PluggableServiceSelector<EngineFactory>() {
			@Override
			public boolean matches(EngineFactory service) {
				if (StringUtils.equalsIgnoreCase(engine, service.getName())) { return true; }
				for (Object alias : service.getAliases()) {
					if (StringUtils.equalsIgnoreCase(engine, Tools.toString(alias))) { return true; }
				}
				return false;
			}
		};

		engineFactories.setHideErrors(false);
		engineFactories.setDefaultSelector(selector);

		try {
			this.engineFactory = engineFactories.getFirst();
		} catch (NoSuchElementException e) {
			throw new CommandLineProcessingException(1,
				String.format("No implementation was found matching engine name or alias [%s]", engine), e);
		}

		// Now go try to initialize the stores if required
		try {
			initializeStores(baseValues);
		} catch (Exception e) {
			throw new CommandLineProcessingException(1, "Failed to initialize the metadata/content stores", e);
		}
	}

	private void initializeStores(OptionValues baseValues) throws Exception {
		if (!this.command.isRequiresStorage()) { return; }

		String path = null;
		if (CLIParam.content.isPresent(baseValues)) {
			path = CLIParam.db.getString(baseValues);
		} else {
			path = Launcher.DEFAULT_DB_PATH;
		}

		final File objectStore = createFile(path);
		if (objectStore.exists() && !objectStore.isFile() && !objectStore.isDirectory()) {
			// ERROR! Not a file or directory! What is this?
			throw new CommandLineProcessingException(1, String.format(
				"The object at path [%s] is neither a file nor a directory - can't use it to describe the metadata store",
				objectStore.getAbsolutePath()));
		}

		// Now we build the content store. If there is no explicit configuration or location, and
		// the either object store's destination is a folder (existent or not), configure the
		// content store relative to the object store

		if (CLIParam.content.isPresent(baseValues)) {
			path = CLIParam.content.getString(baseValues);
		} else {
			if (!objectStore.isFile()) {
				path = null;
			} else {
				path = Launcher.DEFAULT_CONTENT_PATH;
			}
		}

		final File contentStore = ((path != null) ? createFile(path) : new File(objectStore, "content"));
		if (contentStore.exists() && !contentStore.isFile() && !contentStore.isDirectory()) {
			// ERROR! Not a file or directory! What is this?
			throw new CommandLineProcessingException(1, String.format(
				"The object at path [%s] is neither a file nor a directory - can't use it to describe the content store",
				objectStore.getAbsolutePath()));

		}

		CmfStores.initializeConfigurations();

		Map<String, String> commonValues = new HashMap<>();
		if (!objectStore.exists() || objectStore.isDirectory()) {
			commonValues.put("dir.content", objectStore.getAbsolutePath());
		}
		if (!contentStore.exists() || contentStore.isDirectory()) {
			commonValues.put("dir.metadata", contentStore.getAbsolutePath());
		}
		commonValues.put("db.name", "caliente");
		commonValues.put(CmfStoreFactory.CFG_CLEAN_DATA, String.valueOf(this.command.isRequiresCleanData()));

		StoreConfiguration cfg = CmfStores.getObjectStoreConfiguration("default");
		if (objectStore.isFile()) {
			applyStoreProperties(cfg, loadStoreProperties("object", objectStore.getAbsolutePath()));
		}
		cfg.getSettings().putAll(commonValues);
		this.objectStore = CmfStores.createObjectStore(cfg);

		final boolean directFsExport = CLIParam.direct_fs.isPresent(baseValues);

		final String contentStoreName = (directFsExport ? "direct" : "default");
		cfg = CmfStores.getContentStoreConfiguration(contentStoreName);
		if (!directFsExport) {
			String strategy = CLIParam.content_strategy.getString(baseValues);
			if (StringUtils.isBlank(strategy)) {
				strategy = Launcher.DEFAULT_CONTENT_STRATEGY;
			}
			if (!StringUtils.isBlank(strategy)) {
				cfg.getSettings().put("dir.content.strategy", strategy);
			}
			if (contentStore.isFile()) {
				applyStoreProperties(cfg, loadStoreProperties("content", contentStore.getAbsolutePath()));
			}
		}
		cfg.getSettings().putAll(commonValues);
		this.contentStore = CmfStores.createContentStore(cfg);

		// Set the filesystem location where files will be created or read from
		this.log.info(String.format("Using database directory: [%s]", objectStore.getAbsolutePath()));

		// Set the filesystem location where the content files will be created or read from
		this.log.info(String.format("Using content directory: [%s]", contentStore.getAbsolutePath()));
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
		final String engine = this.engineFactory.getName();
		String logMode = StringUtils.lowerCase(command);
		String logEngine = StringUtils.lowerCase(engine);
		String logTimeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		String logName = commandValues.getString(CLIParam.log);
		if (logName == null) {
			logName = Launcher.DEFAULT_LOG_FORMAT;
		}
		System.setProperty("logName", logName);
		System.setProperty("logTimeStamp", logTimeStamp);
		System.setProperty("logMode", logMode);
		System.setProperty("logEngine", logEngine);
		String logCfg = CLIParam.log_cfg.getString(baseValues);
		boolean customLog = false;
		if (logCfg != null) {
			final File cfg = createFile(logCfg);
			if (cfg.exists() && cfg.isFile() && cfg.canRead()) {
				DOMConfigurator.configureAndWatch(Tools.canonicalize(cfg).getAbsolutePath());
				customLog = true;
			}
		}
		if (!customLog) {
			URL config = Thread.currentThread().getContextClassLoader().getResource("log4j.xml");
			if (config != null) {
				DOMConfigurator.configure(config);
			} else {
				config = Thread.currentThread().getContextClassLoader().getResource("log4j.properties");
				if (config != null) {
					PropertyConfigurator.configure(config);
				}
			}
		}

		// Make sure log4j is configured
		Logger.getRootLogger().info("Logging active");
		final Logger console = Logger.getLogger("console");
		console
			.info(String.format("Launching Caliente v%s %s mode for engine %s%n", Caliente.VERSION, command, engine));
		Runtime runtime = Runtime.getRuntime();
		console.info(String.format("Current heap size: %d MB", runtime.totalMemory() / 1024 / 1024));
		console.info(String.format("Maximum heap size: %d MB", runtime.maxMemory() / 1024 / 1024));
		return true;
	}

	@Override
	protected int run(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws Exception {
		return new Caliente().run(this.objectStore, this.contentStore, this.engineFactory, this.command, commandValues,
			positionals);
	}
}