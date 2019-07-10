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
package com.armedia.caliente.tools;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import javax.jcr.Repository;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.launcher.AbstractExecutable;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.LibLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.caliente.content.JcrOakTest;
import com.armedia.caliente.content.MongoRepository;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.CheckedSupplier;

/**
 * This class is used as a testbed to run quick'n'dirty DFC test programs
 *
 *
 *
 */
public class Scratchpad extends AbstractExecutable {

	static final int MIN_TESTS = 1000;
	static final int DEFAULT_TESTS = 10000;

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
	protected int execute(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws Exception {
		final int threads = this.threadsLaunchHelper.getThreads(baseValues, 10);
		final int tests = baseValues.getInteger(CLIParam.test_count);
		final CheckedSupplier<Repository, Exception> repository = new MongoRepository();
		new JcrOakTest(threads, tests, repository).call();
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