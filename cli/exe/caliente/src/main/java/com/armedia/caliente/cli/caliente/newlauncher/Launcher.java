package com.armedia.caliente.cli.caliente.newlauncher;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

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

	private final LibLaunchHelper libLaunchHelper = new LibLaunchHelper();

	// Saves us quite a few keystrokes ;)
	@SuppressWarnings("rawtypes")
	private EngineFactory engineFactory = null;

	private CommandModule command = null;

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
	}

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionValues baseValues, String command,
		OptionValues commandValues, Collection<String> positionals) {
		List<LaunchClasspathHelper> l = new ArrayList<>();
		l.add(this.libLaunchHelper);
		for (LaunchClasspathHelper h : this.engineFactory.getClasspathHelpers()) {
			l.add(h);
		}
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
			final File cfg = new File(logCfg);
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
		return new Caliente().run(this.engineFactory, this.command, commandValues, positionals);
	}
}