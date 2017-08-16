package com.armedia.caliente.cli.newlauncher;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.Parameter;
import com.armedia.caliente.cli.ParameterScheme;
import com.armedia.caliente.cli.classpath.ClasspathPatcher;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.token.StaticTokenSource;
import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.cli.token.TokenLoader;
import com.armedia.caliente.cli.token.TokenSourceRecursionLoopException;

public abstract class AbstractLauncher {
	protected static final Logger LOG = LoggerFactory.getLogger(AbstractLauncher.class);
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * <p>
	 * Process the command-line commandLineParameters. Return {@code 0} if everything is OK and
	 * execution should continue, any other value otherwise. This same value will be used as the
	 * return code for the {@link #launch(boolean, ParameterScheme, String...)} invocation.
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

	protected final int launch(ParameterScheme scheme, String... args) {
		return launch(true, scheme, args);
	}

	protected void initLogging(CommandLineValues cl) {
		// By default, do nothing...
	}

	protected ParameterScheme getNextParameterScheme(CommandLineValues cli) {
		return null;
	}

	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(CommandLineValues cli) {
		return Collections.emptyList();
	}

	protected Parameter findParameter(Token token, ParameterScheme scheme) {
		return null;
	}

	protected final int launch(boolean supportsHelp, ParameterScheme initialScheme, String... args) {
		if (initialScheme == null) { throw new IllegalArgumentException(
			"Must provide an initial parameter scheme to parse against"); }

		CommandLineValues cl = null;
		ParameterScheme scheme = initialScheme;
		Parameter parameter = null;
		TokenLoader tokenLoader = new TokenLoader(new StaticTokenSource("main", Arrays.asList(args)));
		Token token = null;

		nextScheme: while (true) {
			if (scheme == null) {
				scheme = getNextParameterScheme(cl);
				if (scheme == null) {
					// No more schemes, can't parse the rest of the command line...
					break nextScheme;
				}
			}

			nextToken: while (true) {
				if (token == null) {
					try {
						if (!tokenLoader.hasNext()) {
							// No more tokens...nothing left to parse...
							break nextScheme;
						}

						// More tokens? Only pull the next token when needed...
						token = tokenLoader.next();
					} catch (TokenSourceRecursionLoopException | IOException e) {
						// We have a problem...we can't continue!
						throw new RuntimeException("Failed to resolve all the tokens in the command line", e);
					}
				}

				if (token.getType() == Token.Type.STRING) {
					if (parameter != null) {
						if (parameter.getMaxValueCount() != 0) {
							// Split the string, and set the value as the arguments. If it's a
							// repeat within this scheme, then append the arguments. If the argument
							// count is violated, then have a problem
						} else {
							// This is a standalone argument, so parameters to it are by definition
							// unrecognized, so we have a problem...
						}
					} else {
						// This is not a flag, so it must be a positional, which means this scheme
						// is done-done
					}
				} else {
					if (parameter != null) {
						// TODO: Validate that the parameter count requirements for p are met,
						// and we can move on. If they're not, then we have a "syntax" issue
					}

					parameter = findParameter(token, scheme);
					if (parameter != null) {
						// If this parameter has been specified before in this parameter set, then
						// we have to mark it as the current one and keep going...
						continue nextToken;
					}
				}

				// This is the first unrecognized token, so we make a note of it,
				// and loop up to run the next set of parameters, feeding them the
				// current values
				scheme = null;
				continue nextScheme;
			}
		}

		// Check to see if all required parameters are present

		// We're done parsing, so we gather up all remaining strings and explode if there's an
		// unknown flag in play
		List<String> positionals = new ArrayList<>();
		for (Token t : tokenLoader) {
			if (t.getType() == Token.Type.STRING) {
				positionals.add(t.getValue());
				continue;
			}

			// TODO: Enable this...
			// throw new UnknownParameterException(t);
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
}