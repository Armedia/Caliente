package com.armedia.caliente.cli.ticketdecoder;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.LibLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.commons.utilities.Tools;

public class Launcher extends AbstractLauncher {

	public static final void main(String... args) {
		System.exit(new Launcher().launch(args));
	}

	private final LibLaunchHelper libLaunchHelper = new LibLaunchHelper();
	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);
	private final ThreadsLaunchHelper threadsLaunchHelper = new ThreadsLaunchHelper();

	private Launcher() {
	}

	@Override
	protected OptionScheme getOptionScheme() {
		return new OptionScheme(getProgramName()) //
			.addGroup( //
				this.libLaunchHelper.asGroup() //
			) //
			.addGroup( //
				this.dfcLaunchHelper.asGroup() //
			) //
			.addGroup( //
				this.threadsLaunchHelper.asGroup() //
			) //
			.addFrom( //
				Option.unwrap(CLIParam.values()) //
			) //
		;
	}

	@Override
	protected boolean initLogging(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) {
		final String logTimeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());

		// Make sure the log directory always uses forward slashes
		System.setProperty("logName", baseValues.getString(CLIParam.log, CLIConst.DEFAULT_LOG_FORMAT));
		System.setProperty("logTimeStamp", logTimeStamp);

		String logCfg = baseValues.getString(CLIParam.log_config);
		boolean customLog = false;
		if (logCfg != null) {
			final File cfg = new File(logCfg);
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
		Runtime runtime = Runtime.getRuntime();
		console.info("Current heap size: {} MB", runtime.totalMemory() / 1024 / 1024);
		console.info("Maximum heap size: {} MB", runtime.maxMemory() / 1024 / 1024);

		return true;
	}

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionValues baseValues, String command,
		OptionValues commandValies, Collection<String> positionals) {
		return Arrays.asList(this.libLaunchHelper, this.dfcLaunchHelper);
	}

	@Override
	protected String getProgramName() {
		return "caliente-history";
	}

	@Override
	protected int run(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws Exception {
		return new DctmTicketDecoder(this.dfcLaunchHelper, this.threadsLaunchHelper).run(baseValues);
	}
}