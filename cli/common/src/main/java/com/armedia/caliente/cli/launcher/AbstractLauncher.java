package com.armedia.caliente.cli.launcher;

import java.io.Console;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.classpath.ClasspathPatcher;
import com.armedia.caliente.cli.parser.CommandLine;
import com.armedia.caliente.cli.parser.CommandLineParameter;
import com.armedia.caliente.cli.parser.CommandLineParseException;
import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.ParameterDefinition;

public abstract class AbstractLauncher {
	protected static final Logger LOG = LoggerFactory.getLogger(AbstractLauncher.class);
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * <p>
	 * Returns the commandLineParameters definitions to be used in the given pass. If {@code null}
	 * is returned, or an empty {@link Collection}, then the parsing cycle will be broken and no
	 * further command parameter parsing will be performed. The {@code pass} argument is guaranteed
	 * to always be increasing, starting with {@code 0}.
	 * </p>
	 *
	 * @param commandLine
	 * @param pass
	 * @return the collection of {@link ParameterDefinition} instances to use in the next parsing
	 *         pass
	 */
	protected abstract Collection<? extends ParameterDefinition> getCommandLineParameters(CommandLineValues commandLine,
		int pass);

	/**
	 * <p>
	 * Process the command-line commandLineParameters. Return {@code 0} if everything is OK and
	 * execution should continue, any other value otherwise. This same value will be used as the
	 * return code for the {@link #launch(boolean, String...)} invocation.
	 * </p>
	 *
	 * @param commandLine
	 * @return {@code 0} if everything is OK and execution should continue, any other value
	 *         otherwise.
	 */
	protected int processCommandLine(CommandLineValues commandLine) {
		return 0;
	}

	/**
	 * <p>
	 * Returns the name to be given to this executable after the given command parsing pass. The
	 * {@code pass} argument is guaranteed to always be increasing, starting with {@code 0}.
	 * </p>
	 *
	 * @param pass
	 * @return the name to be used for this executable
	 */
	protected abstract String getProgramName(int pass);

	protected Collection<URL> getClasspathPatchesPre(CommandLineValues commandLine) {
		return Collections.emptyList();
	}

	protected Collection<URL> getClasspathPatchesPost(CommandLineValues commandLine) {
		return Collections.emptyList();
	}

	protected final int launch(String... args) {
		return launch(true, args);
	}

	protected void initLogging(CommandLineValues cl) {
		// By default, do nothing...
	}

	protected final int launch(boolean supportsHelp, String... args) {

		// This loop subclasses a chance to cleanly break the parameter parsing loop, while
		// also affording them the opportunity to modify the parameter availability based on
		// previously parsed commandLineParameters
		CommandLine cl = new CommandLine(supportsHelp);
		int pass = -1;
		while (true) {
			Collection<? extends ParameterDefinition> parameters = getCommandLineParameters(cl, ++pass);
			if ((parameters == null) || parameters.isEmpty()) {
				break;
			}

			for (ParameterDefinition def : parameters) {
				try {
					cl.define(def);
				} catch (Exception e) {
					throw new RuntimeException(
						String.format("Failed to initialize the command-line parser (pass # %d)", pass + 1), e);
				}
			}

			try {
				cl.parse(getProgramName(pass), args);
				if (cl.isHelpRequested()) {
					System.err.printf("%s%n", cl.getHelpMessage());
					return 1;
				}
			} catch (CommandLineParseException e) {
				if (e.getHelp() != null) {
					System.err.printf("%s%n", e.getHelp());
				}
				return 1;
			}
		}

		// Process the commandLineParameters given...
		int rc = processCommandLine(cl);
		if (rc != 0) { return rc; }

		Collection<URL> extraPatches = getClasspathPatchesPre(cl);
		if ((extraPatches != null) && !extraPatches.isEmpty()) {
			for (URL u : extraPatches) {
				if (u != null) {
					try {
						ClasspathPatcher.addToClassPath(u);
					} catch (Exception e) {
						throw new RuntimeException(
							String.format("Failed to apply the a-priori classpath patch [%s]", u), e);
					}
				}
			}
		}

		ClasspathPatcher.discoverPatches(false);

		extraPatches = getClasspathPatchesPost(cl);
		if ((extraPatches != null) && !extraPatches.isEmpty()) {
			for (URL u : extraPatches) {
				if (u != null) {
					try {
						ClasspathPatcher.addToClassPath(u);
					} catch (Exception e) {
						throw new RuntimeException(
							String.format("Failed to apply the a-posteriori classpath patch [%s]", u), e);
					}
				}
			}
		}

		// We have a complete command line, and the final classpath. Let's initialize
		// the logging.
		initLogging(cl);

		// The logging is initialized, we can make use of it now.
		for (String s : ClasspathPatcher.getAdditions()) {
			this.log.info("Classpath addition: [{}]", s);
		}

		try {
			return run(cl);
		} catch (Exception e) {
			this.log.error("Exception caught", e);
			return 1;
		}
	}

	protected abstract int run(CommandLineValues commandLine) throws Exception;

	protected final String getPassword(CommandLineValues cli, ParameterDefinition param, String prompt,
		Object... promptParams) {
		CommandLineParameter cliParam = null;
		if ((cli != null) && (param != null)) {
			cliParam = cli.getParameterFromDefinition(cliParam);
		}
		return getPassword(cliParam, prompt, promptParams);
	}

	protected final String getPassword(CommandLineParameter param, String prompt, Object... promptParams) {
		final Console console = System.console();
		if (param != null) { return param.getString(); }
		if (console == null) { return null; }
		if (prompt == null) {
			prompt = "Password:";
		}
		char[] pass = console.readPassword(prompt, promptParams);
		if (pass != null) { return new String(pass); }
		return null;
	}
}