package com.armedia.caliente.cli.caliente.newlauncher;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.CommandScheme;
import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionParseResult;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionSchemeExtensionSupport;
import com.armedia.caliente.cli.OptionValue;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.CommandLineProcessingException;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.LibLaunchHelper;
import com.armedia.commons.utilities.Tools;

public class Launcher extends AbstractLauncher {

	public static final void main(String... args) {
		System.exit(new Launcher().launch(args));
	}

	@Override
	protected OptionSchemeExtensionSupport getSchemeExtensionSupport() {
		return new CalienteOptionSchemeExtension();
	}

	public static final String DEFAULT_LOG_FORMAT = "caliente-${logEngine}-${logMode}-${logTimeStamp}";

	private final OptionGroup logOptions = new OptionGroupImpl("Logging") //
		.add( //
			new OptionImpl() //
				.setLongOpt("log") //
				.setMinArguments(1) //
				.setMaxArguments(1) //
				.setArgumentName("log-name-mask") //
				.setDefault(Launcher.DEFAULT_LOG_FORMAT) //
				.setDescription("") //
	) //
	;

	private final LibLaunchHelper libLaunchHelper = new LibLaunchHelper();
	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionValues baseValues, String command,
		OptionValues commandValues, Collection<String> positionals) {
		List<LaunchClasspathHelper> l = new ArrayList<>();
		l.add(this.libLaunchHelper);
		// Is DFC in play? Add the DFC helper?
		OptionValue engineValue = null;
		if (engineValue.hasValues()) {
			String engineName = engineValue.getString();
			if (StringUtils.equalsIgnoreCase(engineName, "")) {
				l.add(this.dfcLaunchHelper);
			}
		}
		return l;
	}

	@Override
	protected OptionScheme getOptionScheme() {
		// TODO: Shall we implement dynamic command support?
		return new CommandScheme(getProgramName(), true) //
			.addCommand(new Command("import", "imp", "im")) //
			.addCommand(new Command("export", "exp", "ex")) //
			.addCommand(new Command("count", "cnt", "cn")) //
			.addCommand(new Command("encrypt", "enc")) //
			.addCommand(new Command("decrypt", "dec")) //
			.add(this.libLaunchHelper.asGroup()) //
			.add( //
				new OptionGroupImpl("Basic Options") //
					.add( //
						new OptionImpl() //
							.setShort) //
		)
		/*
		.add( //
			null //
		) //
		.add( //
			null //
		) //
		.add( //
			null //
		) //
		*/
		;
	}

	@Override
	protected void processCommandLineResult(OptionParseResult commandLine) throws CommandLineProcessingException {
		// Validate all parameters...make sure everything is kosher, etc...
	}

	@Override
	protected boolean initLogging(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) {
		String engine = null;
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
		String logCfg = CLIParam.log_cfg.getString();
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
		console.info(String.format("Launching Caliente v%s %s mode for engine %s%n", Caliente.VERSION,
			CLIParam.mode.getString(), engine));
		Runtime runtime = Runtime.getRuntime();
		console.info(String.format("Current heap size: %d MB", runtime.totalMemory() / 1024 / 1024));
		console.info(String.format("Maximum heap size: %d MB", runtime.maxMemory() / 1024 / 1024));
		return true;
	}

	@Override
	protected int run(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws Exception {
		return new Caliente().run(command, commandValues, positionals);
	}

	@Override
	protected String getProgramName() {
		return "caliente";
	}
}