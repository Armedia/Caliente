package com.armedia.caliente.tools;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.LibLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.caliente.content.JcrOakTest;
import com.armedia.commons.utilities.Tools;

/**
 * This class is used as a testbed to run quick'n'dirty DFC test programs
 *
 * @author diego.rivera@armedia.com
 *
 */
public class Scratchpad extends AbstractLauncher {

	static final int MIN_TESTS = 1000;
	static final int DEFAULT_TESTS = 100000;

	public static final void main(String... args) {
		System.exit(new Scratchpad().launch(args));
	}

	private final LibLaunchHelper libLaunchHelper = new LibLaunchHelper();
	// private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);
	private final ThreadsLaunchHelper threadsLaunchHelper = new ThreadsLaunchHelper();

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionValues baseValues, String command,
		OptionValues commandValies, Collection<String> positionals) {
		// return Arrays.asList(this.dfcLaunchHelper);
		return Collections.emptyList();
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
	protected String getProgramName() {
		return "Caliente Scratchpad";
	}

	@Override
	protected int run(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws Exception {
		new JcrOakTest(this.threadsLaunchHelper.getThreads(baseValues, 10),
			baseValues.getInteger(CLIParam.test_count)).call();
		return 0;
	}

	@Override
	protected OptionScheme getOptionScheme() {
		return new OptionScheme(getProgramName()) //
			.addGroup( //
				this.libLaunchHelper.asGroup() //
			) //
			/*
			.addGroup( //
				this.dfcLaunchHelper.asGroup() //
			) //
			*/
			.addGroup( //
				this.threadsLaunchHelper.asGroup() //
			) //
			.addFrom( //
				Option.unwrap(CLIParam.values()) //
			) //
		;
	}
}