package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.armedia.caliente.cli.exception.CommandLineSyntaxException;
import com.armedia.caliente.cli.exception.HelpRequestedException;
import com.armedia.caliente.cli.exception.IllegalOptionValuesException;
import com.armedia.caliente.cli.exception.InsufficientOptionValuesException;
import com.armedia.caliente.cli.exception.InsufficientPositionalValuesException;
import com.armedia.caliente.cli.exception.MissingRequiredCommandException;
import com.armedia.caliente.cli.exception.MissingRequiredOptionsException;
import com.armedia.caliente.cli.exception.TooManyOptionValuesException;
import com.armedia.caliente.cli.exception.TooManyPositionalValuesException;
import com.armedia.caliente.cli.exception.UnknownCommandException;
import com.armedia.caliente.cli.exception.UnknownOptionException;
import com.armedia.caliente.cli.token.StaticTokenSource;
import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.cli.token.TokenLoader;
import com.armedia.commons.utilities.Tools;

public class OptionParser {

	public static final boolean DEFAULT_ALLOW_RECURSION = true;
	public static final Character DEFAULT_VALUE_SEPARATOR = TokenLoader.DEFAULT_VALUE_SEPARATOR;

	protected static final Pattern DEFAULT_COMMAND_PATTERN = Pattern.compile("^[\\w&&[^\\d]]\\w*$");

	private static final String VALUE_SEPARATOR_PATTERN = "(?<!\\\\)\\Q%s\\E";

	private static final List<String> NO_POSITIONALS = Collections.emptyList();

	private final Option findOption(OptionScheme scheme, Token token) {
		switch (token.getType()) {
			case LONG_OPTION:
				return scheme.getOption(token.getValue());
			case SHORT_OPTION:
				return scheme.getOption(token.getValue().charAt(0));
			default:
				return null;
		}
	}

	protected Command findCommand(OptionValues currentValues, CommandScheme commandScheme, String commandName) {
		return commandScheme.getCommand(commandName);
	}

	protected boolean isCommand(String str) {
		return (str != null) && OptionParser.DEFAULT_COMMAND_PATTERN.matcher(str).matches();
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		String... args) throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, args);
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		boolean allowRecursion, String... args) throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, allowRecursion, args);
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		char optionValueSplitter, String... args) throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, optionValueSplitter, args);

	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		boolean allowRecursion, char optionValueSplitter, String... args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, allowRecursion, optionValueSplitter, args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		DynamicOptionSchemeSupport dynamicSupport, String... args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(helpOption, baseScheme, dynamicSupport, OptionParser.DEFAULT_ALLOW_RECURSION,
			OptionParser.DEFAULT_VALUE_SEPARATOR, args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		DynamicOptionSchemeSupport dynamicSupport, boolean allowRecursion, String... args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(helpOption, baseScheme, dynamicSupport, allowRecursion, OptionParser.DEFAULT_VALUE_SEPARATOR,
			args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		DynamicOptionSchemeSupport dynamicSupport, char optionValueSplitter, String... args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(helpOption, baseScheme, dynamicSupport, OptionParser.DEFAULT_ALLOW_RECURSION, optionValueSplitter,
			args);

	}

	public final OptionParseResult parse(final Option helpOption, OptionScheme baseScheme,
		final DynamicOptionSchemeSupport dynamicSupport, final boolean allowRecursion, final char optionValueSplitter,
		String... args) throws CommandLineSyntaxException, HelpRequestedException {
		return parse(helpOption, baseScheme, dynamicSupport, allowRecursion, optionValueSplitter,
			(args == null ? OptionParser.NO_POSITIONALS : Arrays.asList(args)));
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		Collection<String> args) throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, args);
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		boolean allowRecursion, Collection<String> args) throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, allowRecursion, args);
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		char optionValueSplitter, Collection<String> args) throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, optionValueSplitter, args);

	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		boolean allowRecursion, char optionValueSplitter, Collection<String> args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, allowRecursion, optionValueSplitter, args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		DynamicOptionSchemeSupport dynamicSupport, Collection<String> args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(helpOption, baseScheme, dynamicSupport, OptionParser.DEFAULT_ALLOW_RECURSION,
			OptionParser.DEFAULT_VALUE_SEPARATOR, args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		DynamicOptionSchemeSupport dynamicSupport, boolean allowRecursion, Collection<String> args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(helpOption, baseScheme, dynamicSupport, allowRecursion, OptionParser.DEFAULT_VALUE_SEPARATOR,
			args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		DynamicOptionSchemeSupport dynamicSupport, char optionValueSplitter, Collection<String> args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(helpOption, baseScheme, dynamicSupport, OptionParser.DEFAULT_ALLOW_RECURSION, optionValueSplitter,
			args);
	}

	private <T extends CommandLineSyntaxException> void raiseExceptionWithHelp(boolean helpRequested,
		OptionScheme baseScheme, Command command, T error) throws CommandLineSyntaxException, HelpRequestedException {
		if (helpRequested) { throw new HelpRequestedException(baseScheme, command, error); }
		if (error != null) { throw error; }
	}

	public final OptionParseResult parse(final Option helpOption, OptionScheme baseScheme,
		final DynamicOptionSchemeSupport dynamicSupport, final boolean allowRecursion, final char optionValueSplitter,
		Collection<String> args) throws CommandLineSyntaxException, HelpRequestedException {

		if ((args == null) || args
			.isEmpty()) { return new OptionParseResult(new OptionValues(), null, null, OptionParser.NO_POSITIONALS); }

		if (baseScheme == null) {
			baseScheme = new OptionScheme("(ad-hoc)");
		}
		if (helpOption != null) {
			baseScheme.addOrReplace(helpOption);
		}

		final TokenLoader tokenLoader = new TokenLoader(new StaticTokenSource("main", args), optionValueSplitter,
			allowRecursion);

		final OptionValues baseValues = new OptionValues();
		final List<Token> positionals = new ArrayList<>();
		final CommandScheme commandScheme = CommandScheme.castAs(baseScheme);

		boolean extensible = (baseScheme.isExtensible() && (dynamicSupport != null));

		Command command = null;
		String commandName = null;
		OptionValues commandValues = null;

		OptionScheme currentScheme = baseScheme;

		Option currentOption = null;
		Option lastOption = null;
		boolean currentOptionFromCommand = false;

		Map<String, Option> baseWithArgs = new HashMap<>();
		Map<String, Option> commandWithArgs = new HashMap<>();

		OptionSchemeExtension extensibleScheme = (extensible ? new OptionSchemeExtension(baseScheme) : null);

		boolean helpRequested = false;

		int extensions = 0;
		nextToken: for (Token token : tokenLoader) {
			selector: switch (token.getType()) {
				case STRING: // A plain string...

					// Is this a parameter to an option?
					if (currentOption != null) {
						// This can only be a string option(s) for currentOption so add an
						// occurrence and validate the schema limits (empty strings are allowed)
						final int maxArgs = currentOption.getMaxArguments();
						if (maxArgs == 0) {
							raiseExceptionWithHelp(helpRequested, baseScheme, command,
								new TooManyOptionValuesException(currentScheme, currentOption, token));
						}

						// Find how many values the option currently has, and check if they're too
						// many
						OptionValues target = (currentOptionFromCommand ? commandValues : baseValues);
						final int existing = target.getValueCount(currentOption);

						// Allow for escaping the separator character with \
						// (i.e. a\,b,c -> [ "a,b", "c" ])
						List<String> values = new ArrayList<>();
						String str = token.getRawString();

						final Character sep = currentOption.getValueSep();
						Set<String> badValues = null;
						if (sep == null) {
							// No value separation is supported or desired, so use the whole string
							// as a single value
							if (!currentOption.isValueAllowed(str)) {
								badValues = Collections.singleton(str);
							}
							values.add(str);
						} else {
							final Pattern valueSplitter = Pattern
								.compile(String.format(OptionParser.VALUE_SEPARATOR_PATTERN, sep));
							Matcher m = valueSplitter.matcher(str);
							int start = 0;
							nextValue: while (m.find()) {
								String nextValue = str.substring(start, m.start());
								if (!currentOption.isValueAllowed(nextValue)) {
									if (badValues == null) {
										badValues = new TreeSet<>();
									}
									badValues.add(nextValue);
									continue nextValue;
								}

								values.add(currentOption.canonicalizeValue(nextValue));
								start = m.end();
							}

							// Add the last value - or total value if there were no splitters
							String lastValue = str.substring(start);
							if (!currentOption.isValueAllowed(lastValue)) {
								if (badValues == null) {
									badValues = new TreeSet<>();
								}
								badValues.add(lastValue);
							}
							values.add(lastValue);
						}

						// If there are invalid values, then we raise the exception
						if (badValues != null) {
							raiseExceptionWithHelp(helpRequested, baseScheme, command,
								new IllegalOptionValuesException(currentScheme, currentOption, badValues));
						}

						// If this would exceed the maximum allowed of option values, then we have a
						// problem (this should NEVER happen for options that don't support
						// arguments, since options are only "remembered" if they support
						// arguments...
						if ((maxArgs > 0) && ((values.size() + existing) > maxArgs)) {
							raiseExceptionWithHelp(helpRequested, baseScheme, command,
								new TooManyOptionValuesException(currentScheme, currentOption, token));
						}

						// No schema violation on the upper end, so we simply add the values
						target.add(currentOption, values);
						(currentOptionFromCommand ? commandWithArgs : baseWithArgs).put(currentOption.getKey(),
							currentOption);

						// At this point, we're for sure no longer processing a option from
						// before...
						currentOption = null;
						continue nextToken;
					}

					// This can either be a positional, a command, or even an error b/c it's out of
					// place
					if (commandScheme != null) {

						// If commands are supported, then the first standalone string option
						// MUST be a command name
						if (command == null) {
							// Find the command...if not a command, then it must be a positional
							// Give out the currently-accumulated option values to assist the
							// search
							currentScheme = command = findCommand(extensible ? baseValues : null, commandScheme,
								token.getRawString());
							// If there is no command
							if (command != null) {
								commandName = command.getName();
								commandValues = new OptionValues();
								extensible = commandScheme.isExtensible() && (dynamicSupport != null);
								if (extensible) {
									extensibleScheme = new OptionSchemeExtension(command);
								}
								if (helpOption != null) {
									// Make sure there is no colliding option from the command...
									command.remove(helpOption);
								}
								lastOption = null;
								continue nextToken;
							}

							// This is an unknown command, so this is an error
							raiseExceptionWithHelp(helpRequested, baseScheme, command,
								new UnknownCommandException(commandScheme, token));
						}
					}

					// This can only be a positional value, as we either don't support commands, or
					// already have one active
					final int maxArgs = currentScheme.getMaxArguments();

					// Are positionals allowed?
					if (!currentScheme.isSupportsPositionals()) {
						raiseExceptionWithHelp(helpRequested, baseScheme, command,
							new TooManyPositionalValuesException(currentScheme, token));
					}

					// If there's an upper limit, check it...
					if ((maxArgs > 0) && (maxArgs <= positionals.size())) {
						raiseExceptionWithHelp(helpRequested, baseScheme, command,
							new TooManyPositionalValuesException(currentScheme, token));
					}

					// We won't know if we've met the lower limit until the end... so postpone until
					// then
					positionals.add(token);
					break selector;

				case LONG_OPTION:
				case SHORT_OPTION:
					// Either a short or long option...
					Option p = null;

					// First things first: is this the help option?
					if (!helpRequested && (helpOption != null)) {
						String expected = null;
						String actual = token.getValue();
						switch (token.getType()) {
							case LONG_OPTION:
								expected = helpOption.getLongOpt();
								break;
							case SHORT_OPTION:
								expected = helpOption.getShortOpt().toString();
								break;
							default:
								expected = null;
								break;
						}
						if (!baseScheme.isCaseSensitive()) {
							expected = expected.toUpperCase();
							actual = actual.toUpperCase();
						}
						helpRequested = Tools.equals(expected, actual);
					}

					// May not have positionals yet, as these would be out-of-place strings
					if (!positionals.isEmpty()) {
						final CommandLineSyntaxException err;
						if (lastOption != null) {
							err = new TooManyOptionValuesException(currentScheme, lastOption, token);
						} else {
							err = new UnknownOptionException(currentScheme, positionals.get(0));
						}
						raiseExceptionWithHelp(helpRequested, baseScheme, command, err);
					}

					boolean fromCommand = false;
					boolean mayExtend = true;
					inner: while (true) {

						// If we're already processing a command, then the remaining options belong
						// to it
						if (command != null) {
							p = findOption(commandScheme, token);
							fromCommand = true;
						}
						// If we're not processing a command, then the options are still part of the
						// base scheme
						if (p == null) {
							p = findOption(baseScheme, token);
							fromCommand = false;
						}

						// We found our option, so we break the dynamic loop
						if (p != null) {
							break inner;
						}

						if (extensible && mayExtend) {
							// Dynamic support enabled!! Try to expand the currently-active
							// scheme

							// Make sure we clear the "modified" flag...
							// extensibleScheme.clearModified();

							// Try to extend the scheme
							dynamicSupport.extendDynamicScheme(extensions, baseValues, command.getName(), commandValues,
								token, extensibleScheme);

							// If there were changes, then we can go back around...
							if (extensibleScheme.isModified()) {
								mayExtend = false;
								continue inner;
							}
						}

						// No such option - neither on the command nor the base scheme, and the
						// extension mechanism didn't fix this....so this is an error
						raiseExceptionWithHelp(helpRequested, baseScheme, command,
							new UnknownOptionException(currentScheme, token));
					}

					// Mark this as the "last" option seen
					lastOption = p;
					currentOptionFromCommand = fromCommand;
					if (p.getMaxArguments() != 0) {
						// This option supports parameters, so track it so we can add them
						currentOption = p;
					} else {
						// This option doesn't support parameters, so don't track it as such
						currentOption = null;
						(fromCommand ? commandValues : baseValues).add(p);
					}
					break selector;
			}
		}

		// Do we require a command, and is it missing?
		if ((commandScheme != null) && commandScheme.isCommandRequired() && (command == null)) {
			raiseExceptionWithHelp(helpRequested, baseScheme, command,
				new MissingRequiredCommandException(commandScheme));
		}

		// Do we have all the required options for both the global and command?
		Collection<Option> baseFaults = new ArrayList<>();
		Collection<Option> commandFaults = new ArrayList<>();
		for (Option p : baseScheme.getRequiredOptions()) {
			if (!baseValues.isPresent(p)) {
				baseFaults.add(p);
			}
		}
		if (command != null) {
			for (Option p : command.getRequiredOptions()) {
				if (!commandValues.isPresent(p)) {
					commandFaults.add(p);
				}
			}
		}
		// If we have faults from missing required options, raise the error!
		if (!baseFaults.isEmpty() || !commandFaults.isEmpty()) {
			raiseExceptionWithHelp(helpRequested, baseScheme, command,
				new MissingRequiredOptionsException(currentScheme, baseFaults, commandName, commandFaults));
		}

		// Validate the minimum arguments for the options that have arguments
		for (Option p : baseWithArgs.values()) {
			if ((p.getMinArguments() > 0) && (baseValues.getValueCount(p) < p.getMinArguments())) {
				baseFaults.add(p);
			}
		}
		for (Option p : commandWithArgs.values()) {
			if ((p.getMinArguments() > 0) && (commandValues.getValueCount(p) < p.getMinArguments())) {
				commandFaults.add(p);
			}
		}
		if (!baseFaults.isEmpty() || !commandFaults.isEmpty()) {
			Option p = (!baseFaults.isEmpty() ? baseFaults.iterator().next() : commandFaults.iterator().next());
			raiseExceptionWithHelp(helpRequested, baseScheme, command,
				new InsufficientOptionValuesException(currentScheme, p));
		}

		// Validate that we have enough positionals as required
		if (currentScheme.getMinArguments() > positionals.size()) {
			raiseExceptionWithHelp(helpRequested, baseScheme, command,
				new InsufficientPositionalValuesException(currentScheme));
		}

		// At this point, we have all the required options, all the options with minimum
		// values have been validated to be compliant, and we know we're not breaking the maximum
		// values limit because that's checked above as we process. Now we validate the positionals

		// This appears to be a schema-correct command line, so clean everything up and return the
		// result so it can be processed

		// Unwrap the positionals...
		List<String> positionalStrings = new ArrayList<>(positionals.size());
		for (Token t : positionals) {
			positionalStrings.add(t.getRawString());
		}

		raiseExceptionWithHelp(helpRequested, baseScheme, command, null);
		return new OptionParseResult(baseValues, (command != null ? command.getName() : null), commandValues,
			positionalStrings);
	}

	/**
	 * <p>
	 * This method is useful if one wishes to process each individual type of
	 * {@link CommandLineSyntaxException} that may be raised as part of an invocation to the
	 * {@code parse()} family of methods, when a {@link HelpRequestedException} is the result of the
	 * invocation.
	 * </p>
	 * <p>
	 * If the given exception is {@code null}, or its {@link HelpRequestedException#getCause()
	 * getCause()} method returns {@code null}, then this method does nothing.
	 * </p>
	 * <p>
	 * This method is equivalent to invoking {@link #splitException(CommandLineSyntaxException)
	 * splitException(}{@link HelpRequestedException#getCause()
	 * e.getCause()}{@link #splitException(CommandLineSyntaxException) )}.
	 * </p>
	 *
	 *
	 * @param e
	 *            the help exception to respond to
	 * @throws IllegalOptionValuesException
	 * @throws InsufficientOptionValuesException
	 * @throws MissingRequiredCommandException
	 * @throws MissingRequiredOptionsException
	 * @throws TooManyOptionValuesException
	 * @throws TooManyPositionalValuesException
	 * @throws UnknownCommandException
	 * @throws UnknownOptionException
	 */
	public void splitException(HelpRequestedException e)
		throws IllegalOptionValuesException, InsufficientOptionValuesException, MissingRequiredCommandException,
		MissingRequiredOptionsException, TooManyOptionValuesException, TooManyPositionalValuesException,
		UnknownCommandException, UnknownOptionException {
		if (e == null) { return; }
		splitException(e.getCause());
	}

	/**
	 * <p>
	 * This method is useful if one wishes to process each individual type of
	 * {@link CommandLineSyntaxException} that may be raised as part of an invocation to the
	 * {@code parse()} family of methods.
	 * </p>
	 * <p>
	 * If the given exception is {@code null}, then this method does nothing.
	 * </p>
	 *
	 * @param e
	 * @throws IllegalOptionValuesException
	 * @throws InsufficientOptionValuesException
	 * @throws MissingRequiredCommandException
	 * @throws MissingRequiredOptionsException
	 * @throws TooManyOptionValuesException
	 * @throws TooManyPositionalValuesException
	 * @throws UnknownCommandException
	 * @throws UnknownOptionException
	 */
	public void splitException(CommandLineSyntaxException e)
		throws IllegalOptionValuesException, InsufficientOptionValuesException, MissingRequiredCommandException,
		MissingRequiredOptionsException, TooManyOptionValuesException, TooManyPositionalValuesException,
		UnknownCommandException, UnknownOptionException {
		if (e == null) { return; }
		if (IllegalOptionValuesException.class.isInstance(e)) { throw IllegalOptionValuesException.class.cast(e); }
		if (InsufficientOptionValuesException.class
			.isInstance(e)) { throw InsufficientOptionValuesException.class.cast(e); }
		if (MissingRequiredCommandException.class
			.isInstance(e)) { throw MissingRequiredCommandException.class.cast(e); }
		if (MissingRequiredOptionsException.class
			.isInstance(e)) { throw MissingRequiredOptionsException.class.cast(e); }
		if (TooManyOptionValuesException.class.isInstance(e)) { throw TooManyOptionValuesException.class.cast(e); }
		if (TooManyPositionalValuesException.class
			.isInstance(e)) { throw TooManyPositionalValuesException.class.cast(e); }
		if (UnknownCommandException.class.isInstance(e)) { throw UnknownCommandException.class.cast(e); }
		if (UnknownOptionException.class.isInstance(e)) { throw UnknownOptionException.class.cast(e); }
	}
}