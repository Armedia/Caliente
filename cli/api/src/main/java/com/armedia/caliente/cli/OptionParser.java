package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.armedia.caliente.cli.exception.InsufficientPositionalValuesException;
import com.armedia.caliente.cli.exception.MissingRequiredOptionException;
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

	private static final String SEPARATOR_PATTERN = "(?<!\\\\)\\Q%s\\E";

	private static final OptionScheme NULL_SCHEME = new OptionScheme("(no-options)");

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
		String... args) throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(null, baseScheme, dynamicSupport, args);
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		boolean allowRecursion, String... args)
		throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(null, baseScheme, dynamicSupport, allowRecursion, args);
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		char optionValueSplitter, String... args)
		throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(null, baseScheme, dynamicSupport, optionValueSplitter, args);

	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		boolean allowRecursion, char optionValueSplitter, String... args)
		throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(null, baseScheme, dynamicSupport, allowRecursion, optionValueSplitter, args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		DynamicOptionSchemeSupport dynamicSupport, String... args)
		throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(helpOption, baseScheme, dynamicSupport, OptionParser.DEFAULT_ALLOW_RECURSION,
			OptionParser.DEFAULT_VALUE_SEPARATOR, args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		DynamicOptionSchemeSupport dynamicSupport, boolean allowRecursion, String... args)
		throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(helpOption, baseScheme, dynamicSupport, allowRecursion, OptionParser.DEFAULT_VALUE_SEPARATOR,
			args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		DynamicOptionSchemeSupport dynamicSupport, char optionValueSplitter, String... args)
		throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(helpOption, baseScheme, dynamicSupport, OptionParser.DEFAULT_ALLOW_RECURSION, optionValueSplitter,
			args);

	}

	public final OptionParseResult parse(final Option helpOption, OptionScheme baseScheme,
		final DynamicOptionSchemeSupport dynamicSupport, final boolean allowRecursion, final char optionValueSplitter,
		String... args) throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(helpOption, baseScheme, dynamicSupport, allowRecursion, optionValueSplitter,
			(args == null ? OptionParser.NO_POSITIONALS : Arrays.asList(args)));
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		Collection<String> args)
		throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(null, baseScheme, dynamicSupport, args);
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		boolean allowRecursion, Collection<String> args)
		throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(null, baseScheme, dynamicSupport, allowRecursion, args);
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		char optionValueSplitter, Collection<String> args)
		throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(null, baseScheme, dynamicSupport, optionValueSplitter, args);

	}

	public final OptionParseResult parse(final OptionScheme baseScheme, DynamicOptionSchemeSupport dynamicSupport,
		boolean allowRecursion, char optionValueSplitter, Collection<String> args)
		throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(null, baseScheme, dynamicSupport, allowRecursion, optionValueSplitter, args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		DynamicOptionSchemeSupport dynamicSupport, Collection<String> args)
		throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(helpOption, baseScheme, dynamicSupport, OptionParser.DEFAULT_ALLOW_RECURSION,
			OptionParser.DEFAULT_VALUE_SEPARATOR, args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		DynamicOptionSchemeSupport dynamicSupport, boolean allowRecursion, Collection<String> args)
		throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(helpOption, baseScheme, dynamicSupport, allowRecursion, OptionParser.DEFAULT_VALUE_SEPARATOR,
			args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		DynamicOptionSchemeSupport dynamicSupport, char optionValueSplitter, Collection<String> args)
		throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {
		return parse(helpOption, baseScheme, dynamicSupport, OptionParser.DEFAULT_ALLOW_RECURSION, optionValueSplitter,
			args);

	}

	public final OptionParseResult parse(final Option helpOption, OptionScheme baseScheme,
		final DynamicOptionSchemeSupport dynamicSupport, final boolean allowRecursion, final char optionValueSplitter,
		Collection<String> args)
		throws UnknownOptionException, UnknownCommandException, TooManyPositionalValuesException,
		TooManyOptionValuesException, InsufficientPositionalValuesException, MissingRequiredOptionException {

		if ((args == null) || args
			.isEmpty()) { return new OptionParseResult(new OptionValues(), null, null, OptionParser.NO_POSITIONALS); }

		baseScheme = Tools.coalesce(baseScheme, OptionParser.NULL_SCHEME);
		final TokenLoader tokenLoader = new TokenLoader(new StaticTokenSource("main", args), optionValueSplitter,
			allowRecursion);
		final Pattern splitter = Pattern
			.compile(String.format(OptionParser.SEPARATOR_PATTERN, tokenLoader.getValueSeparator()));

		final OptionValues baseValues = new OptionValues();
		final List<Token> positionals = new ArrayList<>();
		final CommandScheme commandScheme = (CommandScheme.class.isInstance(baseScheme)
			? CommandScheme.class.cast(baseScheme) : null);

		final boolean dynamic = (dynamicSupport != null);

		Command command = null;
		String commandName = null;
		OptionValues commandValues = null;

		OptionScheme currentScheme = baseScheme;

		Option currentOption = null;
		boolean currentOptionFromCommand = false;

		Map<String, Option> baseWithArgs = new HashMap<>();
		Map<String, Option> commandWithArgs = new HashMap<>();

		ExtensibleOptionScheme extensibleScheme = (dynamic ? new ExtensibleOptionScheme(baseScheme) : null);

		int extensions = 0;
		for (Token nextToken : tokenLoader) {

			if (nextToken.getType() == Token.Type.STRING) {
				// A plain string...
				if (currentOption == null) {
					// This can either be a positional, a command, or an error

					if (commandScheme != null) {
						// If commands are supported, then the first standalone string option
						// MUST be the command name
						if (command == null) {
							// Find the command...if not a command, then it must be a positional
							// Give out the currently-accumulated option values to assist the
							// search
							currentScheme = command = findCommand(dynamic ? baseValues : null, commandScheme,
								nextToken.getRawString());
							if (command != null) {
								commandName = command.getName();
								commandValues = new OptionValues();
								if (dynamic) {
									extensibleScheme = new ExtensibleOptionScheme(command);
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
					if (currentScheme
						.isSupportsPositionals()) { throw new TooManyPositionalValuesException(nextToken); }

					// If there's an upper limit, check it...
					if ((maxArgs > 0)
						&& (maxArgs < positionals.size())) { throw new TooManyPositionalValuesException(nextToken); }

					// We won't know if we've met the lower limit until the end... so postpone until
					// then
					positionals.add(nextToken);
				} else {
					// This can only be a string option(s) for currentOption so add an
					// occurrence and validate the schema limits (empty strings are allowed)
					final int maxArgs = currentOption.getMaxValueCount();
					if (maxArgs == 0) { throw new TooManyOptionValuesException(currentOption, nextToken); }

					// Find how many values the option currently has, and check if they're too
					// many
					OptionValues target = (currentOptionFromCommand ? commandValues : baseValues);
					final int existing = target.getValueCount(currentOption);

					// Allow for escaping the separator character with \
					// (i.e. a\,b,c -> [ "a,b", "c" ])
					List<String> values = new ArrayList<>();
					String str = nextToken.getRawString();
					Matcher m = splitter.matcher(str);
					int start = 0;
					while (m.find()) {
						values.add(str.substring(start, m.start()));
						start = m.end();
					}
					// Add the last value - or total value if there were no splitters
					values.add(str.substring(start));

					// If this would exceed the maximum allowed of option values, then we have a
					// problem
					if ((maxArgs > 0)
						&& ((values.size() + existing) > maxArgs)) { throw new TooManyOptionValuesException(
							currentOption, nextToken); }

					// No schema violation on the upper end, so we simply add the values
					target.add(currentOption, values);
					(currentOptionFromCommand ? commandWithArgs : baseWithArgs).put(currentOption.getKey(),
						currentOption);
				}

				// At this point, we're for sure no longer processing a option from before...
				currentOption = null;
			} else {
				// Either a short or long option...

				// May not have positionals yet, as these would be out-of-place strings
				if (!positionals.isEmpty()) { throw new UnknownOptionException(positionals.get(0)); }

				Option p = null;
				int pass = -1;
				boolean fromCommand = false;
				inner: while (true) {
					pass++;

					// If we're already processing a command, then the remaining options belong
					// to it
					if (command != null) {
						p = findOption(commandScheme, nextToken);
						fromCommand = true;
					}
					// If we're not processing a command, then the options are still part of the
					// base scheme
					if (p == null) {
						p = findOption(baseScheme, nextToken);
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

						// No such option - neither on the command nor the base scheme, and the
						// extension mechanism didn't fix this....so this is an error
						throw new UnknownOptionException(nextToken);
					}

					break;
				}

				if (p.getMaxValueCount() != 0) {
					currentOption = p;
					currentOptionFromCommand = fromCommand;
				}
			}
		}

		// Do we have enough positionals to meet the schema requirements?
		if (currentScheme.getMinArgs() > 0) {
			if (positionals.size() < currentScheme
				.getMinArgs()) { throw new InsufficientPositionalValuesException(null); }
		}

		// Do we have all the required options for both the global and command?
		Collection<Option> baseFaults = new ArrayList<>();
		for (Option p : baseScheme.getRequiredOptions()) {
			if (!baseValues.isPresent(p)) {
				baseFaults.add(p);
			}
		}
		Collection<Option> commandFaults = new ArrayList<>();
		if (command != null) {
			for (Option p : command.getRequiredOptions()) {
				if (!commandValues.isPresent(p)) {
					commandFaults.add(p);
				}
			}
		}

		// If we have faults from missing required options, raise the error!
		if (!baseFaults.isEmpty() || !commandFaults.isEmpty()) { throw new MissingRequiredOptionException(baseFaults,
			commandName, commandFaults); }

		// Validate the minimum arguments for the options that have arguments
		for (Option p : baseWithArgs.values()) {
			if ((p.getMinValueCount() > 0) && (baseValues.getValueCount(p) < p.getMinValueCount())) {
				baseFaults.add(p);
			}
		}
		for (Option p : commandWithArgs.values()) {
			if ((p.getMinValueCount() > 0) && (commandValues.getValueCount(p) < p.getMinValueCount())) {
				commandFaults.add(p);
			}
		}
		if (!baseFaults.isEmpty() || !commandFaults.isEmpty()) {
			// TODO: RAISE THE ERROR FOR OPTIONS WITH MISSING VALUES ON BOTH SCHEMES!!
		}

		// At this point, we have all the required options, all the options with minimum
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

		return new OptionParseResult(baseValues, command.getName(), commandValues, positionalStrings);
	}
}