package com.armedia.caliente.cli.launcher;

import java.io.Console;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.classpath.ClasspathPatcher;
import com.armedia.caliente.cli.parser.CommandLine;
import com.armedia.caliente.cli.parser.CommandLineParseException;
import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.Parameter;

public abstract class AbstractLauncher {
	protected static final Logger LOG = LoggerFactory.getLogger(AbstractLauncher.class);
	protected final Logger log = LoggerFactory.getLogger(getClass());

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

	protected final int launch(String... args) {
		return launch(true, args);
	}

	protected void initLogging(CommandLineValues cl) {
		// By default, do nothing...
	}

	protected Collection<? extends LaunchParameterSet> getLaunchParameterSets(CommandLineValues cli, int pass) {
		return Collections.emptyList();
	}

	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(CommandLineValues cli) {
		return Collections.emptyList();
	}

	protected final int launch(boolean supportsHelp, String... args) {
		/*
		- find @xxxxx -> read parameters from @xxxxx and graft them into the command line at that location (i.e. insert the parameters from the file)
			- one line = one parameter position (?)
			- support quoting for multi-lined values (?)
		
		
		
		while (true) {
			add new parameters/groups for this pass;
			if (no new parameters or groups) break;
		
			parse what's known
		
			if (required params missing || parameter values missing) {
			// Malformed = requires arguments but doesn't have any, doesn't support arguments but has one, etc...
				store errors;
				break;
			}
		
		// Loop for the next pass
		}
		
		if (has errors || help requested) {
			// show error or help message
			return 1
		}
		 */

		// This loop subclasses a chance to cleanly break the parameter parsing loop, while
		// also affording them the opportunity to modify the parameter availability based on
		// previously parsed commandLineParameters
		CommandLine cl = new CommandLine(supportsHelp);
		int pass = -1;
		final Collection<Parameter> newParameters = new ArrayList<>();
		while (true) {
			newParameters.clear();
			// If there are any helpers to be applied, we do so now
			Collection<? extends LaunchParameterSet> launchParameterSets = getLaunchParameterSets(cl, ++pass);
			if (launchParameterSets != null) {
				for (LaunchParameterSet parameterSet : launchParameterSets) {
					final Collection<? extends Parameter> p = parameterSet.getParameters(cl);
					if ((p != null) && !p.isEmpty()) {
						newParameters.addAll(p);
					}
				}
			}

			// If there are no new parameters, there's no point in re-parsing, so we move on...
			if (newParameters.isEmpty()) {
				break;
			}

			// We have new parameters, so we define them...
			for (Parameter def : newParameters) {
				try {
					cl.define(def);
				} catch (Exception e) {
					throw new RuntimeException(
						String.format("Failed to initialize the command-line parser (pass # %d)", pass + 1), e);
				}
			}

			// Now, we re-parse with the new parameter set...
			try {
				cl.parse(getProgramName(pass), args);

				// TODO: We may need to modify the help support so that it doesn't bork out on the
				// first pass, but instead we can do 2 or 3 or more passes and display help for
				// those as we go... we may need to rethink how this is supported...
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

		Collection<? extends LaunchClasspathHelper> classpathHelpers = getClasspathHelpers(cl);
		if (classpathHelpers == null) {
			Collections.emptyList();
		}

		for (LaunchClasspathHelper helper : classpathHelpers) {
			final Collection<URL> extraPatches = helper.getClasspathPatchesPre(cl);
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
		}

		ClasspathPatcher.discoverPatches(false);

		for (LaunchClasspathHelper helper : classpathHelpers) {
			final Collection<URL> extraPatches = helper.getClasspathPatchesPost(cl);
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

	protected final String getPassword(CommandLineValues cli, Parameter param, String prompt,
		Object... promptParams) {
		// If the parameter is given, return its value
		if ((cli != null) && (param != null) && cli.isPresent(param)) { return cli.getString(param); }

		final Console console = System.console();
		if (console == null) { return null; }

		// If the parameter isn't given, but a console is available, ask for a password
		// interactively
		if (prompt == null) {
			prompt = "Password:";
		}
		char[] pass = console.readPassword(prompt, promptParams);
		if (pass != null) { return new String(pass); }
		return null;
	}
}