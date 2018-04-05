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
import com.armedia.caliente.cli.OptionWrapper;
import com.armedia.caliente.cli.classpath.ClasspathPatcher;
import com.armedia.caliente.cli.exception.CommandLineSyntaxException;
import com.armedia.caliente.cli.exception.HelpRequestedException;
import com.armedia.caliente.cli.help.HelpRenderer;
import com.armedia.caliente.cli.launcher.log.LogConfigurator;
import com.armedia.commons.utilities.Tools;

public abstract class AbstractLauncher {

	private static final Option HELP_OPTION = new OptionImpl() //
		.setShortOpt('?') //
		.setLongOpt("help") //
		.setMinArguments(0) //
		.setMaxArguments(0) //
		.setDescription("Display this help message") //
	;

	private static final Logger BOOT_LOG = LogConfigurator.getBootLogger();

	private static final String[] NO_ARGS = {};

	protected Logger log = AbstractLauncher.BOOT_LOG;
	protected Logger console = AbstractLauncher.BOOT_LOG;

	protected final File userDir;
	protected final File homeDir;

	protected AbstractLauncher() {
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
	 * will be raised, and the invocation to {@link #launch(String...)} will return the value
	 * obtained from that exception's {@link CommandLineProcessingException#getReturnValue()
	 * getReturnValue()}.
	 * </p>
	 *
	 * @throws CommandLineProcessingException
	 *             if there was an error processing the command line - such as an illegal option
	 *             combination, illegal option value, etc
	 */
	protected void processCommandLineResult(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws CommandLineProcessingException {
	}

	protected final int launch(String... args) {
		return launch(AbstractLauncher.HELP_OPTION, args);
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
		return new OptionParser().parse(helpOption, baseScheme, getSchemeExtensionSupport(), args);
	}

	protected OptionSchemeExtensionSupport getSchemeExtensionSupport() {
		// By default return nothing...
		return null;
	}

	protected abstract OptionScheme getOptionScheme();

	protected final int launch(OptionWrapper helpOption, String... args) {
		return launch(Option.unwrap(helpOption), args);
	}

	protected final int launch(Option helpOption, String... args) {
		final OptionScheme optionScheme;
		try {
			optionScheme = getOptionScheme();
		} catch (Exception e) {
			this.log.error("Failed to initialize the option scheme", e);
			return -1;
		}

		if (args == null) {
			args = AbstractLauncher.NO_ARGS;
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
		for (String s : ClasspathPatcher.getAdditions()) {
			this.log.info("Classpath addition: [{}]", s);
		}

		try {
			return run(result.getOptionValues(), result.getCommand(), result.getCommandValues(),
				result.getPositionals());
		} catch (Exception e) {
			this.log.error("Exception caught", e);
			return 1;
		}
	}

	protected abstract int run(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws Exception;
}