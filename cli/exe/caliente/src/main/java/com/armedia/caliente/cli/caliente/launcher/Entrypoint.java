/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
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
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.CommandScheme;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.CalienteCommand;
import com.armedia.caliente.cli.caliente.command.CommandModule;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.cli.exception.CommandLineSyntaxException;
import com.armedia.caliente.cli.exception.DynamicOptionsException;
import com.armedia.caliente.cli.filter.StringValueFilter;
import com.armedia.caliente.cli.launcher.AbstractEntrypoint;
import com.armedia.caliente.cli.launcher.CommandLineProcessingException;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.LibLaunchHelper;
import com.armedia.caliente.engine.tools.HierarchicalOrganizer;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfStoreFactory;
import com.armedia.caliente.store.CmfStores;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.Tools;

public class Entrypoint extends AbstractEntrypoint {

	/**
	 * Read the Caliente version... is this the cleanest way?
	 */
	public static final String VERSION;
	static {
		String version = null;
		URL url = Thread.currentThread().getContextClassLoader().getResource("version.properties");
		if (url != null) {
			try (InputStream in = url.openStream()) {
				Properties p = new Properties();
				p.load(in);
				version = p.getProperty("version");
			} catch (IOException e) {
				e.printStackTrace(System.err);
				version = "(failed to load)";
			}
		}
		VERSION = Tools.coalesce(version, "(unknown)");
	}

	public static final CmfCrypt CRYPTO = new CmfCrypt();
	public static final String STORE_PROP_CONTENT_LOCATION_REQUIRED = "caliente.content.location.required";

	private static final String STORE_TYPE_PROPERTY = "caliente.store.type";
	private static final Path DEFAULT_DATA_PATH = Paths.get("caliente");
	private static final String DEFAULT_DB_PATH = "db";
	private static final String DEFAULT_STREAMS_PATH = "streams";
	private static final String DEFAULT_LOG_PATH = "logs";

	private static final String DEFAULT_STREAMS_ORGANIZER = HierarchicalOrganizer.NAME;

	private final LibLaunchHelper libLaunchHelper = new LibLaunchHelper();

	// Saves us quite a few keystrokes ;)
	private AbstractEngineInterface engineInterface = null;

	private CommandModule<?> command = null;

	private File baseDataLocation = null;

	private File objectStoreLocation = null;
	private CmfObjectStore<?> objectStore = null;

	private File contentStoreLocation = null;
	private CmfContentStore<?, ?> contentStore = null;

	private File logLocation = null;

	private boolean directFsMode = false;

	private String contentOrganizer = Entrypoint.DEFAULT_STREAMS_ORGANIZER;

	@Override
	protected Option getHelpOption() {
		return CLIParam.help.option;
	}

	@Override
	protected String getProgramName() {
		return "caliente";
	}

	@Override
	protected OptionScheme getOptionScheme() {

		final CommandScheme scheme = new CommandScheme(getProgramName(), true);
		for (CalienteCommand d : CalienteCommand.values()) {
			Command c = new Command(d.getTitle(), d.getAliases()) {

				@Override
				public void initializeDynamicOptions(boolean helpRequested, OptionValues baseValues)
					throws CommandLineSyntaxException {
					String err = initializeEngineAndCommand(baseValues, getName());
					if (err != null) {
						if (!helpRequested) { throw new DynamicOptionsException(this, err); }
					}

					if (DynamicEngineOptions.class.isInstance(Entrypoint.this.engineInterface)) {
						DynamicEngineOptions.class.cast(Entrypoint.this.engineInterface)
							.getDynamicOptions(Entrypoint.this.command.getDescriptor(), this);
					}
					if (DynamicCommandOptions.class.isInstance(Entrypoint.this.command)) {
						DynamicCommandOptions.class.cast(Entrypoint.this.command)
							.getDynamicOptions(Entrypoint.this.engineInterface.getName(), this);
					}
				}

			};
			c.setDescription(d.getDescription());
			scheme.addCommand(c);
		}

		// Now, find the engines available
		OptionImpl impl = OptionImpl.cast(CLIParam.engine);
		if (impl != null) {
			Collection<String> engines = AbstractEngineInterface.getAliases(this.log);
			if (engines.isEmpty()) { throw new IllegalStateException("No engine interfaces found, cannot continue!"); }
			impl.setValueFilter(new StringValueFilter(false, engines));
		}

		return scheme //
			.addFrom(CLIGroup.BASE) //
		;
	}

	private String initializeEngineAndCommand(OptionValues baseValues, String currentCommand) {
		// Has an engine been selected already?
		if (this.engineInterface == null) {
			if (!baseValues.isPresent(CLIParam.engine)) {
				return "No engine was selected in the base options (option order is important!)";
			}

			// Find the desired engine
			final String engine = baseValues.getString(CLIParam.engine);
			this.engineInterface = AbstractEngineInterface.get(engine);
			if (this.engineInterface == null) {
				return String.format("No engine was found with the name or alias [%s]", engine);
			}
		}

		// Has a command been given yet?
		if (this.command == null) {
			if (StringUtils.isBlank(currentCommand)) { return "No command was given (option order is important!)"; }

			final CalienteCommand calienteCommand = CalienteCommand.get(currentCommand);
			if (calienteCommand == null) {
				return String.format("Command [%s] is not a valid Caliente command or command alias", currentCommand);
			}

			this.command = this.engineInterface.getCommandModule(calienteCommand);
			if (this.command == null) {
				return String.format("Engine [%s] does not support command [%s]", this.engineInterface.getName(),
					calienteCommand.getTitle());
			}
		}

		return null;
	}

	@Override
	protected void processCommandLineResult(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws CommandLineProcessingException {

		final String error = initializeEngineAndCommand(baseValues, command);
		if (error != null) { throw new CommandLineProcessingException(1, error); }

		this.baseDataLocation = getBaseDataLocation(commandValues);
		this.logLocation = getLogLocation(baseValues);
	}

	private File getBaseDataLocation(OptionValues baseValues) throws CommandLineProcessingException {
		// Step 2: There is no special location used by the engine, so see what the user wants to do
		String path = null;
		if (baseValues.isPresent(CLIParam.data)) {
			path = baseValues.getString(CLIParam.data);
		} else {
			path = Entrypoint.DEFAULT_DATA_PATH.toString();
		}

		File f = newCanonicalFile(path);
		if (f.exists() && !f.isFile() && !f.isDirectory()) {
			// ERROR! Not a file or directory! What is this?
			throw new CommandLineProcessingException(1, String.format(
				"The object at path [%s] is neither a file nor a directory - can't use it to describe the data store root",
				f));
		}
		return f;
	}

	private File getMetadataLocation(OptionValues baseValues) throws CommandLineProcessingException {
		// Step 2: There is no special location used by the engine, so see what the user wants to do
		String path = null;
		if (baseValues.isPresent(CLIParam.db)) {
			path = baseValues.getString(CLIParam.db);
		} else {
			path = new File(this.baseDataLocation, Entrypoint.DEFAULT_DB_PATH).getAbsolutePath();
		}

		File f = newCanonicalFile(path);
		if (f.exists() && !f.isFile() && !f.isDirectory()) {
			// ERROR! Not a file or directory! What is this?
			throw new CommandLineProcessingException(1, String.format(
				"The object at path [%s] is neither a file nor a directory - can't use it to describe the metadata db store",
				f));
		}
		return f;
	}

	private StoreConfiguration buildObjectStoreConfiguration(OptionValues commandValues)
		throws IOException, CommandLineProcessingException {
		final File metadataLocation = this.objectStoreLocation;
		final Map<String, String> commonValues = new HashMap<>();
		commonValues.put("dir.root", this.baseDataLocation.getAbsolutePath());

		if (!metadataLocation.exists() || metadataLocation.isDirectory()) {
			commonValues.put("dir.metadata", metadataLocation.getAbsolutePath());
		}
		commonValues.put("db.name", "caliente");

		StoreConfiguration cfg = CmfStores.getObjectStoreConfiguration("default");
		applyStoreProperties(cfg,
			loadStoreProperties("object", metadataLocation.isFile() ? metadataLocation.getAbsolutePath() : null));

		cfg.getSettings().putAll(commonValues);
		this.command.customizeObjectStoreProperties(commandValues, cfg);
		commonValues.put(CmfStoreFactory.CFG_CLEAN_DATA,
			String.valueOf(this.command.getDescriptor().isRequiresCleanData()));
		cfg.getSettings().putAll(commonValues);
		return cfg;
	}

	private File getContentLocation(CmfObjectStore<?> objectStore, OptionValues commandValues)
		throws CommandLineProcessingException {
		final boolean contentLocationWasGiven = commandValues.isPresent(CLIParam.streams);
		final File calculatedContentLocation = Tools
			.canonicalize(new File(this.baseDataLocation, Entrypoint.DEFAULT_STREAMS_PATH));
		final File contentLocation;
		if (contentLocationWasGiven) {
			contentLocation = newCanonicalFile(commandValues.getString(CLIParam.streams));
		} else {
			contentLocation = calculatedContentLocation;
		}
		if (contentLocation.exists() && !contentLocation.isFile() && !contentLocation.isDirectory()) {
			// ERROR! Not a file or directory! What is this?
			throw new CommandLineProcessingException(1, String.format(
				"The object at path [%s] is neither a file nor a directory - can't use it to describe the content store",
				contentLocation));
		}

		// If the option is not present, then check to see if it's required to process
		// this data set (if this is a new data set, this property won't be set so we're fine)
		if (!contentLocationWasGiven) {
			try {
				final CmfValue contentLocationRequired = objectStore
					.getProperty(Entrypoint.STORE_PROP_CONTENT_LOCATION_REQUIRED);
				if ((contentLocationRequired != null) && contentLocationRequired.asBoolean()) {
					throw new CommandLineProcessingException(1, String.format(
						"This extraction doesn't seem to bundle with the content streams; you must provide the required option to point out its location (usually %s)",
						CLIParam.streams.option.getKey()));
				}
			} catch (CmfStorageException e) {
				throw new CommandLineProcessingException(1,
					String.format("Failed to query the property %s from the Object Store",
						Entrypoint.STORE_PROP_CONTENT_LOCATION_REQUIRED),
					e);
			}
		}

		// If this processing mode needs to set the location requirement (exports do),
		// then we calculate the value and store it
		if (this.command.isShouldStoreContentLocationRequirement()) {
			// These two will only be equals if --streams is not given, or --streams is given
			// and its final canonical location matches the calculated default location
			final boolean contentInDefaultLocation = Tools.equals(contentLocation, calculatedContentLocation);

			// The content location flag (--streams) will be required "downstream" (i.e. upon import
			// of the data set being created) if the content streams were NOT stored in the
			// default location during extraction. When this can happen varies engine-to-engine so
			// we make sure to delegate the calculation calculate.
			final boolean contentLocationRequired = !contentInDefaultLocation
				|| this.command.isContentStreamsExternal(commandValues);
			try {
				objectStore.setProperty(Entrypoint.STORE_PROP_CONTENT_LOCATION_REQUIRED,
					new CmfValue(contentLocationRequired));
			} catch (CmfStorageException e) {
				throw new CommandLineProcessingException(1,
					String.format("Failed to store the property %s into the Object Store",
						Entrypoint.STORE_PROP_CONTENT_LOCATION_REQUIRED),
					e);
			}
		}

		return contentLocation;
	}

	private StoreConfiguration buildContentStoreConfiguration(OptionValues commandValues)
		throws IOException, CommandLineProcessingException {

		final boolean directFsExport = this.directFsMode;
		final File contentLocation = this.contentStoreLocation;

		Map<String, String> commonValues = new HashMap<>();
		commonValues.put("dir.root", this.baseDataLocation.getAbsolutePath());

		if (!contentLocation.exists() || contentLocation.isDirectory()) {
			commonValues.put("dir.content", contentLocation.getAbsolutePath());
		}
		commonValues.put("db.name", "caliente");

		final String contentStoreName = (directFsExport ? "direct" : "default");
		StoreConfiguration cfg = CmfStores.getContentStoreConfiguration(contentStoreName);

		String contentOrganizer = this.contentOrganizer;
		if (directFsExport) {
			// Ugly hack, but works...
			this.contentOrganizer = cfg.getEffectiveSettings().get("uri.organizer");
		} else {
			if (StringUtils.isBlank(contentOrganizer)) {
				contentOrganizer = Tools.coalesce(this.command.getContentOrganizerName(commandValues),
					Entrypoint.DEFAULT_STREAMS_ORGANIZER);
			}
			this.contentOrganizer = contentOrganizer;
			applyStoreProperties(cfg,
				loadStoreProperties("content", contentLocation.isFile() ? contentLocation.getAbsolutePath() : null));
		}
		this.command.customizeContentStoreProperties(commandValues, cfg);
		if (!directFsExport) {
			cfg.getSettings().put("dir.content.organizer", this.contentOrganizer);
		}

		commonValues.put(CmfStoreFactory.CFG_CLEAN_DATA,
			String.valueOf(this.command.getDescriptor().isRequiresCleanData()));
		cfg.getSettings().putAll(commonValues);
		return cfg;
	}

	private File getLogLocation(OptionValues baseValues) throws CommandLineProcessingException {
		// Step 1: Does this engine use a special location for content?
		String path = null;
		if (baseValues.isPresent(CLIParam.log_dir)) {
			path = baseValues.getString(CLIParam.log_dir);
		} else {
			path = new File(this.baseDataLocation, Entrypoint.DEFAULT_LOG_PATH).getAbsolutePath();
		}

		File f = newCanonicalFile(path);
		if (f.exists() && !f.isFile() && !f.isDirectory()) {
			// ERROR! Not a file or directory! What is this?
			throw new CommandLineProcessingException(1, String.format(
				"The object at path [%s] is neither a file nor a directory - can't use it to describe the log directory",
				f));
		}
		return f;
	}

	private void initializeStores(OptionValues commandValues) throws Exception {
		if (!this.command.getDescriptor().isRequiresStorage()) { return; }

		// Set the filesystem location where files will be created or read from
		File storeLocation = null;

		CmfStores.initializeConfigurations();

		StoreConfiguration cfg = null;

		this.objectStoreLocation = getMetadataLocation(commandValues);
		cfg = buildObjectStoreConfiguration(commandValues);
		this.objectStore = CmfStores.createObjectStore(cfg);
		storeLocation = this.objectStore.getStoreLocation();
		if (storeLocation != null) {
			this.console.info("Using metadata directory: [{}]", storeLocation.getAbsolutePath());
		} else {
			this.console.info("The Metadata Store does not support local storage");
		}

		this.contentStoreLocation = getContentLocation(this.objectStore, commandValues);
		this.directFsMode = commandValues.isPresent(CLIParam.direct_fs);
		this.contentOrganizer = commandValues.getString(CLIParam.organizer);
		cfg = buildContentStoreConfiguration(commandValues);
		this.contentStore = CmfStores.createContentStore(cfg, this.objectStore);
		storeLocation = this.contentStore.getStoreLocation();
		if (storeLocation != null) {
			this.console.info("Using content directory: [{}]", storeLocation.getAbsolutePath());
		} else {
			this.console.info("The Content Store does not support local storage");
		}
	}

	private void shutdownStores() throws Exception {
		this.console.info("Closing up all internal stores...");
		boolean ok = false;
		try {
			CmfStores.close();
			ok = true;
		} finally {
			this.console.info("The internal data stores were closed{}",
				ok ? " cleanly." : " WITH ERRORS! (check the logs)");
		}
	}

	protected boolean applyStoreProperties(StoreConfiguration cfg, Properties properties) {
		if ((properties == null) || properties.isEmpty()) { return false; }
		String storeType = properties.getProperty(Entrypoint.STORE_TYPE_PROPERTY);
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

	private File newCanonicalFile(String path) {
		return Tools.canonicalize(new File(path));
	}

	protected File locateFile(String path, boolean required) throws IOException {
		File f = newCanonicalFile(path);
		if (!f.exists()) {
			if (required) { throw new IOException(String.format("The file [%s] doesn't exist", f.getAbsolutePath())); }
			return null;
		}

		// We've found the path we're looking for...verify that it's regular file. Otherwise,
		// just ignore it. If this is an explicit configuration setting, then we explode!
		if (!f.isFile()) {
			if (required) {
				throw new IOException(String.format("The file [%s] is not a regular file", f.getAbsolutePath()));
			}
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
		if (this.engineInterface != null) {
			l.addAll(this.engineInterface.getClasspathHelpers());
		}
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
			final File cfg = newCanonicalFile(logCfg);
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
		console.info("Launching Caliente v{} {} mode for engine {}{}", Entrypoint.VERSION, command, engine, Tools.NL);
		Runtime runtime = Runtime.getRuntime();
		console.info("Current heap size: {} MB", runtime.totalMemory() / 1024 / 1024);
		console.info("Maximum heap size: {} MB", runtime.maxMemory() / 1024 / 1024);

		return true;
	}

	@Override
	protected void showBanner(Logger log) {
		log.info("Caliente CLI v{}", Entrypoint.VERSION);
	}

	@Override
	protected int execute(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws Exception {

		try {
			initializeStores(commandValues);

			final CalienteState state = new CalienteState(this.baseDataLocation, this.objectStoreLocation,
				this.objectStore, this.contentStoreLocation, this.contentStore);

			final String engineName = this.engineInterface.getName();
			final Logger log = LoggerFactory.getLogger(getClass());
			final CmfObjectStore<?> objectStore = state.getObjectStore();
			final boolean writeProperties = (objectStore != null);
			final String format = String.format("caliente.%s.%%s",
				this.command.getDescriptor().getTitle().toLowerCase());
			try {
				if (writeProperties) {
					Map<String, CmfValue> properties = new TreeMap<>();
					properties.put(String.format(format, "engine"), new CmfValue(engineName));
					properties.put(String.format(format, "version"), new CmfValue(Entrypoint.VERSION));
					properties.put(String.format(format, "start"), new CmfValue(new Date()));
					objectStore.setProperties(properties);
				}
				this.command.run(state, commandValues, positionals);
			} catch (Throwable t) {
				if (writeProperties) {
					try {
						objectStore.setProperty(String.format(format, "error"), new CmfValue(Tools.dumpStackTrace(t)));
					} catch (Exception e) {
						log.error("Failed to store the captured error into the properties database", e);
					}
				}
				throw new RuntimeException("Execution failed", t);
			} finally {
				// TODO: Unlock from single execution
				try {
					if (writeProperties) {
						objectStore.setProperty(String.format(format, "end"), new CmfValue(new Date()));
					}
				} finally {
					shutdownStores();
				}
			}
			return 0;
		} finally {
			this.command.close();
		}
	}
}