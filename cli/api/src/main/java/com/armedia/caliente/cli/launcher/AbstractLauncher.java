package com.armedia.caliente.cli.launcher;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.CommandLineResult;
import com.armedia.caliente.cli.CommandScheme;
import com.armedia.caliente.cli.InsufficientValuesException;
import com.armedia.caliente.cli.Parameter;
import com.armedia.caliente.cli.ParameterScheme;
import com.armedia.caliente.cli.ParameterValues;
import com.armedia.caliente.cli.ParameterValuesImpl;
import com.armedia.caliente.cli.TooManyParameterValuesException;
import com.armedia.caliente.cli.TooManyValuesException;
import com.armedia.caliente.cli.UnknownCommandException;
import com.armedia.caliente.cli.UnknownParameterException;
import com.armedia.caliente.cli.classpath.ClasspathPatcher;
import com.armedia.caliente.cli.launcher.log.LogConfigurator;
import com.armedia.caliente.cli.token.StaticTokenSource;
import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.cli.token.TokenLoader;

public abstract class AbstractLauncher {

	private static final Logger BOOT_LOG = LogConfigurator.getBootLogger();

	private static final String[] NO_ARGS = {};

	protected Logger log = AbstractLauncher.BOOT_LOG;

	protected static final Pattern DEFAULT_COMMAND_PATTERN = Pattern.compile("^[\\w&&[^\\d]]\\w*$");

	/**
	 * <p>
	 * Process the command-line commandLineParameters. If an error occurs, a
	 * {@link CommandLineProcessingException} will be raised, and the invocation to
	 * {@link #launch(ParameterScheme, String...)} will return the value obtained from that
	 * exception's {@link CommandLineProcessingException#getReturnValue() getReturnValue()}.
	 * </p>
	 *
	 * @param commandLine
	 * @throws CommandLineProcessingException
	 *             if there was an error processing the command line - such as an illegal parameter
	 *             combination, illegal parameter value, etc
	 */
	protected void processCommandLineResult(CommandLineResult commandLine) throws CommandLineProcessingException {
	}

	protected final int launch(ParameterScheme scheme, String... args) {
		return launch(null, scheme, args);
	}

	protected boolean initLogging(CommandLineResult cl) {
		// By default, do nothing...
		return false;
	}

	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(CommandLineResult cli) {
		return Collections.emptyList();
	}

	private Parameter findParameter(ParameterScheme scheme, Token token) {
		switch (token.getType()) {
			case LONG_OPTION:
				return scheme.getParameter(token.getValue());
			case SHORT_OPTION:
				return scheme.getParameter(token.getValue().charAt(0));
			default:
				return null;
		}
	}

	/**
	 * Returns {@code true} if the given string represents a command, {@code false} otherwise. The
	 * default implementation only checks to see that the string isn't {@code null}, and that it
	 * matches the regular expression {@code /^[\w&&[^\d]]\w*$/}. Subclasses are free to extend or
	 * override this method.
	 *
	 * @param str
	 *            the string to check
	 * @return {@code true} if the given string represents a command, {@code false} otherwise
	 */
	protected boolean isCommand(String str) {
		return (str != null) && AbstractLauncher.DEFAULT_COMMAND_PATTERN.matcher(str).matches();
	}

	private CommandLineResult parseArguments(Parameter helpParameter, final ParameterScheme baseScheme, String... args)
		throws UnknownParameterException, UnknownCommandException, TooManyValuesException,
		TooManyParameterValuesException, InsufficientValuesException {

		final DynamicParameterSchemeSupport dynamicSupport = getDynamicSchemeSupport();
		final TokenLoader tokenLoader = new TokenLoader(new StaticTokenSource("main", Arrays.asList(args)));

		final ParameterValuesImpl baseValues = new ParameterValuesImpl(baseScheme);
		final List<Token> positionals = new ArrayList<>();
		final CommandScheme commandScheme = (CommandScheme.class.isInstance(baseScheme)
			? CommandScheme.class.cast(baseScheme) : null);

		final boolean dynamic = (dynamicSupport != null);

		Command command = null;
		ParameterValuesImpl commandValues = null;

		ParameterScheme currentScheme = baseScheme;

		Parameter currentParameter = null;
		boolean currentParameterFromCommand = false;

		Map<String, Parameter> baseWithArgs = new HashMap<>();
		Map<String, Parameter> commandWithArgs = new HashMap<>();

		ExtensibleParameterScheme extensibleScheme = (dynamic ? new ExtensibleParameterScheme(baseScheme) : null);

		int extensions = 0;
		for (Token nextToken : tokenLoader) {

			if (nextToken.getType() == Token.Type.STRING) {
				// A plain string...
				if (currentParameter == null) {
					// This can either be a positional, a command, or an error

					if (commandScheme != null) {
						// If commands are supported, then the first standalone string parameter
						// MUST be the command name
						if (command == null) {
							// Find the command...if not a command, then it must be a positional
							// Give out the currently-accumulated parameter values to assist the
							// search
							currentScheme = command = getCommand(dynamic ? baseValues : null, commandScheme,
								nextToken.getRawString());
							if (command != null) {
								commandValues = new ParameterValuesImpl(command);
								if (dynamic) {
									extensibleScheme = new ExtensibleParameterScheme(command);
								}
								continue;
							}

							// This is an unknown command, so this is an error
							throw new UnknownCommandException(nextToken);
						}
					}

					// This can only be a positional value, as we either don't support commands, or
					// already have one active
					final int maxArgs = currentScheme.getMaxArgs();

					// Are positionals allowed?
					if (maxArgs == 0) { throw new TooManyValuesException(nextToken); }

					// If there's an upper limit, check it...
					if ((maxArgs > 0)
						&& (maxArgs < positionals.size())) { throw new TooManyValuesException(nextToken); }

					// We won't know if we've met the lower limit until the end... so postpone until
					// then
					positionals.add(nextToken);
				} else {
					// This can only be a string parameter(s) for currentParameter so add an
					// occurrence and validate the schema limits (empty strings are allowed)
					int maxArgs = currentParameter.getMaxValueCount();
					if (maxArgs == 0) { throw new TooManyParameterValuesException(currentParameter, nextToken); }

					// Find how many values the parameter currently has, and check if they're too
					// many
					ParameterValuesImpl target = (currentParameterFromCommand ? commandValues : baseValues);
					final int existing = target.getValueCount(currentParameter);

					StrTokenizer tok = new StrTokenizer(nextToken.getRawString(), tokenLoader.getValueSplitter());
					tok.setIgnoreEmptyTokens(false);

					List<String> values = tok.getTokenList();

					// If this would exceed the maximum allowed of parameter values, then we have a
					// problem
					if ((maxArgs > 0)
						&& ((values.size() + existing) > maxArgs)) { throw new TooManyParameterValuesException(
							currentParameter, nextToken); }

					// No schema violation on the upper end, so we simply add the values
					target.add(currentParameter, values);
					(currentParameterFromCommand ? commandWithArgs : baseWithArgs).put(currentParameter.getKey(),
						currentParameter);
				}

				// At this point, we're for sure no longer processing a parameter from before...
				currentParameter = null;
			} else {
				// Either a short or long option...

				// May not have positionals yet, as these would be out-of-place strings
				if (!positionals.isEmpty()) { throw new UnknownParameterException(positionals.get(0)); }

				Parameter p = null;
				int pass = -1;
				boolean fromCommand = false;
				inner: while (true) {
					pass++;

					// If we're already processing a command, then the remaining parameters belong
					// to it
					if (command != null) {
						p = findParameter(commandScheme, nextToken);
						fromCommand = true;
					}
					// If we're not processing a command, then the parameters are still part of the
					// base scheme
					if (p == null) {
						p = findParameter(baseScheme, nextToken);
						fromCommand = false;
					}

					if (p == null) {
						if (dynamic && (pass == 0)) {
							// Dynamic support enabled!! Try to expand the currently-active scheme
							dynamicSupport.extendDynamicScheme(extensions, baseValues, command.getName(), commandValues,
								nextToken, extensibleScheme);

							// If there were changes, then we can go back around...
							if (extensibleScheme.isModified()) {
								continue inner;
							}
						}

						// No such parameter - neither on the command nor the base scheme, and the
						// extension mechanism didn't fix this....so this is an error
						throw new UnknownParameterException(nextToken);
					}

					break;
				}

				if (p.getMaxValueCount() != 0) {
					currentParameter = p;
					currentParameterFromCommand = fromCommand;
				}
			}
		}

		// Do we have enough positionals to meet the schema requirements?
		if (currentScheme.getMinArgs() > 0) {
			if (positionals.size() < currentScheme.getMinArgs()) { throw new InsufficientValuesException(null); }
		}

		// Do we have all the required parameters for both the global and command?
		Collection<Parameter> baseFaults = new ArrayList<>();
		for (Parameter p : baseScheme.getRequiredParameters()) {
			if (!baseValues.isPresent(p)) {
				baseFaults.add(p);
			}
		}
		Collection<Parameter> commandFaults = new ArrayList<>();
		if (command != null) {
			for (Parameter p : command.getRequiredParameters()) {
				if (!commandValues.isPresent(p)) {
					commandFaults.add(p);
				}
			}
		}

		if (!baseFaults.isEmpty() || !commandFaults.isEmpty()) {
			// TODO: RAISE THE ERROR FOR ALL MISSING PARAMETERS ON BOTH SCHEMES!!
		}

		// Validate the minimum parameters for the parameters that have arguments
		for (Parameter p : baseWithArgs.values()) {
			if ((p.getMinValueCount() > 0) && (baseValues.getValueCount(p) < p.getMinValueCount())) {
				baseFaults.add(p);
			}
		}
		for (Parameter p : commandWithArgs.values()) {
			if ((p.getMinValueCount() > 0) && (commandValues.getValueCount(p) < p.getMinValueCount())) {
				commandFaults.add(p);
			}
		}
		if (!baseFaults.isEmpty() || !commandFaults.isEmpty()) {
			// TODO: RAISE THE ERROR FOR PARAMETERS WITH MISSING VALUES ON BOTH SCHEMES!!
		}

		// At this point, we have all the required parameters, all the parameters with minimum
		// values have been validated to be compliant, and we know we're not breaking the maximum
		// values limit because that's checked above as we process. Now we validate the positionals

		if ((currentScheme.getMinArgs() > 0) && (positionals.size() < currentScheme.getMinArgs())) {
			// TODO: RAISE THE ERROR THAT WE'RE MISSING REQUIRED POSITIONALS
		}

		// This appears to be a schema-correct command line, so clean everything up and return the
		// result so it can be processed

		// Unwrap the positionals...
		List<String> positionalStrings = new ArrayList<>(positionals.size());
		for (Token t : positionals) {
			positionalStrings.add(t.getRawString());
		}

		return new CommandLineResult(baseValues, command.getName(), commandValues, positionalStrings);
	}

	private Command getCommand(ParameterValues currentValues, CommandScheme commandScheme, String commandName) {
		return commandScheme.getCommand(commandName);
	}

	protected DynamicParameterSchemeSupport getDynamicSchemeSupport() {
		// By default return nothing...
		return null;
	}

	protected final int launch(Parameter helpParameter, final ParameterScheme parameterScheme, String... args) {
		if (parameterScheme == null) { throw new IllegalArgumentException(
			"Must provide an initial parameter scheme to parse against"); }
		if (args == null) {
			args = AbstractLauncher.NO_ARGS;
		}

		if ((helpParameter != null)
			|| (parameterScheme.countCollisions(helpParameter) != 1)) { throw new IllegalArgumentException(
				"The help parameter is not part of the parameter scheme"); }

		CommandLineResult result = null;
		try {
			result = parseArguments(helpParameter, parameterScheme, args);
		} catch (Throwable t) {
			this.log.error("Failed to process the command-line arguments", t);
			return -1;
		}

		// Process the commandLineParameters given...
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
			final Collection<URL> extraPatches = helper.getClasspathPatchesPre(result.getParameterValues());
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
			final Collection<URL> extraPatches = helper.getClasspathPatchesPost(result.getParameterValues());
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

	protected abstract int run(CommandLineResult commandLine) throws Exception;
}