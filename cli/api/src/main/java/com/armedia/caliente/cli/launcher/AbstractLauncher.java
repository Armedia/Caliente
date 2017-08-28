package com.armedia.caliente.cli.launcher;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.DynamicOptionSchemeSupport;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionParseResult;
import com.armedia.caliente.cli.OptionParser;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.classpath.ClasspathPatcher;
import com.armedia.caliente.cli.exception.CommandLineSyntaxException;
import com.armedia.caliente.cli.exception.HelpRequestedException;
import com.armedia.caliente.cli.launcher.log.LogConfigurator;

public abstract class AbstractLauncher {

	private static final Logger BOOT_LOG = LogConfigurator.getBootLogger();

	private static final String[] NO_ARGS = {};

	protected Logger log = AbstractLauncher.BOOT_LOG;

	/**
	 * <p>
	 * Process the OptionParseResult. If an error occurs, a {@link CommandLineProcessingException}
	 * will be raised, and the invocation to {@link #launch(OptionScheme, String...)} will return
	 * the value obtained from that exception's
	 * {@link CommandLineProcessingException#getReturnValue() getReturnValue()}.
	 * </p>
	 *
	 * @param commandLine
	 * @throws CommandLineProcessingException
	 *             if there was an error processing the command line - such as an illegal option
	 *             combination, illegal option value, etc
	 */
	protected void processCommandLineResult(OptionParseResult commandLine) throws CommandLineProcessingException {
	}

	protected final int launch(OptionScheme scheme, String... args) {
		return launch(null, scheme, args);
	}

	protected boolean initLogging(OptionParseResult cl) {
		// By default, do nothing...
		return false;
	}

	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionParseResult cli) {
		return Collections.emptyList();
	}

	private OptionParseResult parseArguments(Option helpOption, final OptionScheme baseScheme, String... args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return new OptionParser().parse(helpOption, baseScheme, getDynamicSchemeSupport(), args);
	}

	protected DynamicOptionSchemeSupport getDynamicSchemeSupport() {
		// By default return nothing...
		return null;
	}

	protected final int launch(Option helpOption, final OptionScheme optionScheme, String... args) {
		Objects.requireNonNull(optionScheme, "Must provide an initial option scheme to parse against");

		if (args == null) {
			args = AbstractLauncher.NO_ARGS;
		}

		OptionParseResult result = null;
		try {
			result = parseArguments(helpOption, optionScheme, args);
		} catch (HelpRequestedException e) {
			// Help requested...print out the help message!
			System.out.printf("Help requested, to be implemented...%n");
			return 1;
		} catch (Throwable t) {
			this.log.error("Failed to process the command-line arguments", t);
			return -1;
		}

		try {
			processCommandLineResult(result);
		} catch (CommandLineProcessingException e) {
			this.log.error("Failed to process the command-line values", e);
			return e.getReturnValue();
		}

		Collection<? extends LaunchClasspathHelper> classpathHelpers = getClasspathHelpers(result);
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
		if (initLogging(result)) {
			// Retrieve the logger post-initialization...if nothing was initialized, we stick to the
			// same log
			this.log = LoggerFactory.getLogger(getClass());
		}

		// The logging is initialized, we can make use of it now.
		for (String s : ClasspathPatcher.getAdditions()) {
			this.log.info("Classpath addition: [{}]", s);
		}

		try {
			return run(result);
		} catch (Exception e) {
			this.log.error("Exception caught", e);
			return 1;
		}
	}

	protected abstract int run(OptionParseResult commandLine) throws Exception;
}