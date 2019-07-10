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
package com.armedia.caliente.cli.launcher;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionParseResult;
import com.armedia.caliente.cli.OptionParser;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionSchemeExtensionSupport;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.classpath.ClasspathPatcher;
import com.armedia.caliente.cli.exception.CommandLineSyntaxException;
import com.armedia.caliente.cli.exception.HelpRequestedException;
import com.armedia.caliente.cli.help.HelpRenderer;
import com.armedia.commons.utilities.Tools;

public abstract class AbstractExecutable {

	private static final Option HELP_OPTION = new OptionImpl() //
		.setShortOpt('?') //
		.setLongOpt("help") //
		.setMinArguments(0) //
		.setMaxArguments(0) //
		.setDescription("Display this help message") //
	;

	private static final String[] NO_ARGS = {};

	protected Logger log = Main.BOOT_LOG;
	protected Logger console = Main.BOOT_LOG;

	protected final File userDir;
	protected final File homeDir;

	protected AbstractExecutable() {
		String userDir = System.getProperty("user.dir");
		if (StringUtils.isEmpty(userDir)) {
			userDir = ".";
		}
		this.userDir = Tools.canonicalize(new File(userDir));
		String homeDir = System.getProperty("user.home");
		if (StringUtils.isEmpty(homeDir)) {
			homeDir = ".";
		}
		this.homeDir = Tools.canonicalize(new File(homeDir));
	}

	/**
	 * <p>
	 * Process the OptionParseResult. If an error occurs, a {@link CommandLineProcessingException}
	 * will be raised, and the exit result will be set to the value obtained from that exception's
	 * {@link CommandLineProcessingException#getReturnValue() getReturnValue()}.
	 * </p>
	 *
	 * @throws CommandLineProcessingException
	 *             if there was an error processing the command line - such as an illegal option
	 *             combination, illegal option value, etc
	 */
	protected void processCommandLineResult(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws CommandLineProcessingException {
	}

	protected Option getHelpOption() {
		return AbstractExecutable.HELP_OPTION;
	}

	protected abstract String getProgramName();

	protected boolean initLogging(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) {
		// By default, do nothing...
		return false;
	}

	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionValues baseValues, String command,
		OptionValues commandValues, Collection<String> positionals) {
		return Collections.emptyList();
	}

	private OptionParseResult parseArguments(Option helpOption, final OptionScheme baseScheme, String... args)
		throws CommandLineSyntaxException, HelpRequestedException {
		OptionSchemeExtensionSupport extensionSupport = null;
		if (OptionSchemeExtensionSupport.class.isInstance(this)) {
			extensionSupport = OptionSchemeExtensionSupport.class.cast(this);
		}
		return new OptionParser().parse(helpOption, baseScheme, extensionSupport, args);
	}

	protected abstract OptionScheme getOptionScheme();

	public int execute(String... args) {
		final Option helpOption = getHelpOption();
		final OptionScheme optionScheme;
		try {
			optionScheme = getOptionScheme();
		} catch (Exception e) {
			this.log.error("Failed to initialize the option scheme", e);
			return -1;
		}

		if (args == null) {
			args = AbstractExecutable.NO_ARGS;
		}

		OptionParseResult result = null;
		try {
			result = parseArguments(helpOption, optionScheme, args);
		} catch (HelpRequestedException e) {
			HelpRenderer.renderHelp(getProgramName(), e, System.err);
			return 1;
		} catch (CommandLineSyntaxException e) {
			HelpRenderer.renderError("ERROR", e, System.err);
			return 1;
		} catch (Throwable t) {
			this.log.error("Failed to process the command-line arguments", t);
			return -1;
		}

		try {
			processCommandLineResult(result.getOptionValues(), result.getCommand(), result.getCommandValues(),
				result.getPositionals());
		} catch (CommandLineProcessingException e) {
			this.log.error("Failed to process the command-line values", e);
			return e.getReturnValue();
		}

		Collection<? extends LaunchClasspathHelper> classpathHelpers = getClasspathHelpers(result.getOptionValues(),
			result.getCommand(), result.getCommandValues(), result.getPositionals());
		if (classpathHelpers == null) {
			Collections.emptyList();
		}

		for (LaunchClasspathHelper helper : classpathHelpers) {
			final Collection<URL> extraPatches = helper.getClasspathPatchesPre(result.getOptionValues());
			if ((extraPatches != null) && !extraPatches.isEmpty()) {
				for (URL u : extraPatches) {
					if (u != null) {
						try {
							ClasspathPatcher.addToClassPath(u);
						} catch (Exception e) {
							this.log.error("Failed to apply the a-priori classpath patch [{}]", u, e);
							return -1;
						}
					}
				}
			}
		}

		ClasspathPatcher.discoverPatches(false);

		for (LaunchClasspathHelper helper : classpathHelpers) {
			final Collection<URL> extraPatches = helper.getClasspathPatchesPost(result.getOptionValues());
			if ((extraPatches != null) && !extraPatches.isEmpty()) {
				for (URL u : extraPatches) {
					if (u != null) {
						try {
							ClasspathPatcher.addToClassPath(u);
						} catch (Exception e) {
							this.log.error("Failed to apply the a-posteriori classpath patch [{}]", u, e);
							return -1;
						}
					}
				}
			}
		}

		// We have a complete command line, and the final classpath. Let's initialize
		// the logging.
		if (initLogging(result.getOptionValues(), result.getCommand(), result.getCommandValues(),
			result.getPositionals())) {
			// Retrieve the logger post-initialization...if nothing was initialized, we stick to the
			// same log
			this.log = LoggerFactory.getLogger(getClass());
			this.console = LoggerFactory.getLogger("console");
		}

		// The logging is initialized, we can make use of it now.
		showBanner(this.console);
		for (String s : ClasspathPatcher.getAdditions()) {
			this.console.info("Classpath addition: [{}]", s);
		}

		try {
			int ret = execute(result.getOptionValues(), result.getCommand(), result.getCommandValues(),
				result.getPositionals());
			showFooter(this.console, ret);
			return ret;
		} catch (Exception e) {
			showError(this.console, e);
			return 1;
		}
	}

	protected void showBanner(Logger log) {
		// By default, do nothing
	}

	protected void showFooter(Logger log, int rc) {
		// By default, do nothing
	}

	protected void showError(Logger log, Throwable e) {
		log.error("Exception caught", e);
	}

	protected abstract int execute(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws Exception;
}