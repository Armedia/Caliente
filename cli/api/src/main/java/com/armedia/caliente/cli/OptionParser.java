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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

	private class Extender implements OptionSchemeExtender {
		private final OptionScheme baseScheme;
		private final OptionScheme command;

		private boolean modified = false;

		private Extender(OptionScheme baseScheme) {
			this(baseScheme, null);
		}

		private Extender(OptionScheme baseScheme, OptionScheme command) {
			this.baseScheme = baseScheme;
			this.command = command;
		}

		private OptionScheme getScheme() {
			return Tools.coalesce(this.command, this.baseScheme);
		}

		private void resetModified() {
			this.modified = false;
		}

		private boolean isModified() {
			return this.modified;
		}

		@Override
		public Extender addGroup(OptionGroup group) {
			if ((group == null) || (group.getOptionCount() == 0)) { return this; }
			getScheme().addGroup(group);
			this.modified = true;
			return this;
		}

		@Override
		public boolean hasOption(Character option) {
			if (option == null) { return false; }
			if ((this.command != null) && this.command.hasOption(option)) { return true; }
			return this.baseScheme.hasOption(option);
		}

		@Override
		public boolean hasOption(String option) {
			if (option == null) { return false; }
			if ((this.command != null) && this.command.hasOption(option)) { return true; }
			return this.baseScheme.hasOption(option);
		}

		@Override
		public int hasOption(Option option) {
			if (option == null) { return 0; }
			if (this.command != null) {
				int ret = this.command.hasOption(option);
				if (ret != 0) { return ret; }
			}
			return this.baseScheme.hasOption(option);
		}

		@Override
		public boolean hasGroup(String name) {
			return getScheme().hasGroup(name);
		}
	}

	public static final boolean DEFAULT_ALLOW_RECURSION = true;
	public static final Character DEFAULT_VALUE_SEPARATOR = TokenLoader.DEFAULT_VALUE_SEPARATOR;

	protected static final Pattern DEFAULT_COMMAND_PATTERN = Pattern.compile("^[\\w&&[^\\d]]\\w*$");

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

	protected boolean isHelpOption(Option helpOption, Token token) {
		if ((helpOption == null) || (token == null)) { return false; }

		final String shortOpt = (helpOption.getShortOpt() != null ? helpOption.getShortOpt().toString() : null);
		final String longOpt = (helpOption.getLongOpt() != null ? helpOption.getLongOpt().toUpperCase() : null);

		String expected = null;
		String actual = token.getValue();
		switch (token.getType()) {
			case STRING:
				// Do nothing...this isn't the help option
				return false;

			case LONG_OPTION:
				expected = longOpt;
				actual = actual.toUpperCase();
				break;

			case SHORT_OPTION:
				expected = shortOpt;
				break;
		}
		return Tools.equals(expected, actual);
	}

	protected boolean isHelpRequested(Option helpOption, Collection<Token> tokens) {
		if ((helpOption == null) || (tokens == null) || tokens.isEmpty()) { return false; }

		final String shortOpt = (helpOption.getShortOpt() != null ? helpOption.getShortOpt().toString() : null);
		final String longOpt = (helpOption.getLongOpt() != null ? helpOption.getLongOpt().toUpperCase() : null);

		String expected = null;
		for (Token t : tokens) {
			String actual = t.getValue();
			switch (t.getType()) {
				case STRING:
					// Do nothing...this isn't the help option
					continue;
				case LONG_OPTION:
					expected = longOpt;
					actual = actual.toUpperCase();
					break;
				case SHORT_OPTION:
					expected = shortOpt;
					break;
			}
			if (Tools.equals(expected, actual)) { return true; }
		}
		return false;
	}

	protected Command findCommand(OptionValues currentValues, CommandScheme commandScheme, String commandName) {
		return commandScheme.getCommand(commandName);
	}

	protected boolean isCommand(String str) {
		return (str != null) && OptionParser.DEFAULT_COMMAND_PATTERN.matcher(str).matches();
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, OptionSchemeExtensionSupport dynamicSupport,
		String... args) throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, args);
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, OptionSchemeExtensionSupport dynamicSupport,
		boolean allowRecursion, String... args) throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, allowRecursion, args);
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, OptionSchemeExtensionSupport dynamicSupport,
		char optionValueSplitter, String... args) throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, optionValueSplitter, args);

	}

	public final OptionParseResult parse(final OptionScheme baseScheme, OptionSchemeExtensionSupport dynamicSupport,
		boolean allowRecursion, char optionValueSplitter, String... args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, allowRecursion, optionValueSplitter, args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		OptionSchemeExtensionSupport dynamicSupport, String... args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(helpOption, baseScheme, dynamicSupport, OptionParser.DEFAULT_ALLOW_RECURSION,
			OptionParser.DEFAULT_VALUE_SEPARATOR, args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		OptionSchemeExtensionSupport dynamicSupport, boolean allowRecursion, String... args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(helpOption, baseScheme, dynamicSupport, allowRecursion, OptionParser.DEFAULT_VALUE_SEPARATOR,
			args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		OptionSchemeExtensionSupport dynamicSupport, char optionValueSplitter, String... args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(helpOption, baseScheme, dynamicSupport, OptionParser.DEFAULT_ALLOW_RECURSION, optionValueSplitter,
			args);

	}

	public final OptionParseResult parse(final Option helpOption, OptionScheme baseScheme,
		final OptionSchemeExtensionSupport dynamicSupport, final boolean allowRecursion, final char optionValueSplitter,
		String... args) throws CommandLineSyntaxException, HelpRequestedException {
		return parse(helpOption, baseScheme, dynamicSupport, allowRecursion, optionValueSplitter,
			(args == null ? OptionParser.NO_POSITIONALS : Arrays.asList(args)));
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, OptionSchemeExtensionSupport dynamicSupport,
		Collection<String> args) throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, args);
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, OptionSchemeExtensionSupport dynamicSupport,
		boolean allowRecursion, Collection<String> args) throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, allowRecursion, args);
	}

	public final OptionParseResult parse(final OptionScheme baseScheme, OptionSchemeExtensionSupport dynamicSupport,
		char optionValueSplitter, Collection<String> args) throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, optionValueSplitter, args);

	}

	public final OptionParseResult parse(final OptionScheme baseScheme, OptionSchemeExtensionSupport dynamicSupport,
		boolean allowRecursion, char optionValueSplitter, Collection<String> args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(null, baseScheme, dynamicSupport, allowRecursion, optionValueSplitter, args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		OptionSchemeExtensionSupport dynamicSupport, Collection<String> args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(helpOption, baseScheme, dynamicSupport, OptionParser.DEFAULT_ALLOW_RECURSION,
			OptionParser.DEFAULT_VALUE_SEPARATOR, args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		OptionSchemeExtensionSupport dynamicSupport, boolean allowRecursion, Collection<String> args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(helpOption, baseScheme, dynamicSupport, allowRecursion, OptionParser.DEFAULT_VALUE_SEPARATOR,
			args);
	}

	public final OptionParseResult parse(Option helpOption, final OptionScheme baseScheme,
		OptionSchemeExtensionSupport dynamicSupport, char optionValueSplitter, Collection<String> args)
		throws CommandLineSyntaxException, HelpRequestedException {
		return parse(helpOption, baseScheme, dynamicSupport, OptionParser.DEFAULT_ALLOW_RECURSION, optionValueSplitter,
			args);
	}

	private <T extends CommandLineSyntaxException> void raiseExceptionWithHelp(boolean helpRequested, Option helpOption,
		OptionScheme baseScheme, Command command, T error) throws CommandLineSyntaxException, HelpRequestedException {
		if (helpRequested) { throw new HelpRequestedException(helpOption, baseScheme, command, error); }
		if (error != null) { throw error; }
	}

	public final OptionParseResult parse(final Option helpOption, OptionScheme baseScheme,
		final OptionSchemeExtensionSupport extensionSupport, final boolean allowRecursion,
		final char optionValueSplitter, Collection<String> args)
		throws CommandLineSyntaxException, HelpRequestedException {

		final CommandScheme commandScheme = Tools.cast(CommandScheme.class, baseScheme);

		if ((args == null) || args.isEmpty()) {
			// Check for missing required command
			if ((commandScheme != null) && commandScheme.isCommandRequired()) {
				raiseExceptionWithHelp(false, helpOption, baseScheme, null,
					new MissingRequiredCommandException(commandScheme));
			}

			// Check for missing required parameters
			Collection<Option> missing = baseScheme.getOptions().stream().filter(Option::isRequired)
				.collect(Collectors.toCollection(ArrayList::new));
			if (!missing.isEmpty()) {
				raiseExceptionWithHelp(false, helpOption, baseScheme, null,
					new MissingRequiredOptionsException(baseScheme, missing, null, null));
			}

			// Check for missing minimum positionals
			if (baseScheme.getMinArguments() > 0) {
				raiseExceptionWithHelp(false, helpOption, baseScheme, null,
					new InsufficientPositionalValuesException(baseScheme));
			}

			return new OptionParseResult(new OptionValues(), null, null, OptionParser.NO_POSITIONALS);
		}

		if (baseScheme == null) {
			baseScheme = new OptionScheme("(ad-hoc)");
		}

		if (helpOption != null) {
			// Add it to the base scheme, regardless
			baseScheme.remove(helpOption);
			baseScheme.add(helpOption);
		}

		final TokenLoader tokenLoader = new TokenLoader(new StaticTokenSource("main", args), optionValueSplitter,
			allowRecursion);

		final OptionValues baseValues = new OptionValues();
		final List<Token> positionals = new ArrayList<>();

		final boolean extensible = (extensionSupport != null);

		Command command = null;
		String commandName = null;
		OptionValues commandValues = null;

		OptionScheme currentScheme = baseScheme;

		Option currentOption = null;
		Option lastOption = null;
		boolean currentOptionFromCommand = false;

		Map<String, Option> baseWithArgs = new HashMap<>();
		Map<String, Option> commandWithArgs = new HashMap<>();

		Extender extender = (extensible ? new Extender(baseScheme) : null);

		final boolean helpRequested = isHelpRequested(helpOption, tokenLoader.getBaseTokens());

		int extensionCount = 0;
		nextToken: for (Token token : tokenLoader) {
			selector: switch (token.getType()) {
				case STRING: // A plain string...

					// Is this a parameter to an option?
					if (currentOption != null) {
						// This can only be a string option(s) for currentOption so add an
						// occurrence and validate the schema limits (empty strings are allowed)
						final int maxArgs = currentOption.getMaxArguments();
						if (maxArgs == 0) {
							raiseExceptionWithHelp(helpRequested, helpOption, baseScheme, command,
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
							nextValue: for (String nextValue : Tools.splitEscaped(sep, str)) {
								if (!currentOption.isValueAllowed(nextValue)) {
									if (badValues == null) {
										badValues = new TreeSet<>();
									}
									badValues.add(nextValue);
									continue nextValue;
								}

								values.add(nextValue);
							}
						}

						// If there are invalid values, then we raise the exception
						if (badValues != null) {
							raiseExceptionWithHelp(helpRequested, helpOption, baseScheme, command,
								new IllegalOptionValuesException(currentScheme, currentOption, badValues));
						}

						// If this would exceed the maximum allowed of option values, then we have a
						// problem (this should NEVER happen for options that don't support
						// arguments, since options are only "remembered" if they support
						// arguments...
						if ((maxArgs > 0) && ((values.size() + existing) > maxArgs)) {
							raiseExceptionWithHelp(helpRequested, helpOption, baseScheme, command,
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
								command.initializeDynamicOptions(helpRequested, baseValues);

								if (helpOption != null) {
									// If there's a command option that conflicts with the help
									// option, we remove it quietly.
									command.remove(helpOption);
								}

								commandValues = new OptionValues();
								if (extensible) {
									extender = new Extender(baseScheme, command);
								}
								lastOption = null;
								continue nextToken;
							}

							// This is an unknown command, so this is an error
							raiseExceptionWithHelp(helpRequested, helpOption, baseScheme, command,
								new UnknownCommandException(commandScheme, token));
						}
					}

					// This can only be a positional value, as we either don't support commands, or
					// already have one active
					final int maxArgs = currentScheme.getMaxArguments();

					// Are positionals allowed?
					if (!currentScheme.isSupportsPositionals()) {
						raiseExceptionWithHelp(helpRequested, helpOption, baseScheme, command,
							new TooManyPositionalValuesException(currentScheme, token));
					}

					// If there's an upper limit, check it...
					if ((maxArgs > 0) && (maxArgs <= positionals.size())) {
						raiseExceptionWithHelp(helpRequested, helpOption, baseScheme, command,
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
					// May not have positionals yet, as these would be out-of-place strings
					if (!positionals.isEmpty()) {
						final CommandLineSyntaxException err;
						if (lastOption != null) {
							err = new TooManyOptionValuesException(currentScheme, lastOption, token);
						} else {
							err = new UnknownOptionException(currentScheme, positionals.get(0));
						}
						raiseExceptionWithHelp(helpRequested, helpOption, baseScheme, command, err);
					}

					boolean fromCommand = false;
					boolean mayExtend = true;
					inner: while (true) {

						// If we're already processing a command, then the remaining options belong
						// to it
						if (command != null) {
							p = findOption(command, token);
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

							// Try to extend the scheme
							extender.resetModified();
							extensionSupport.extendScheme(++extensionCount, baseValues,
								command != null ? command.getName() : null, commandValues, token, extender);

							// If there were changes, then we can go back around...
							if (extender.isModified()) {
								mayExtend = false;
								continue inner;
							}
						}

						// No such option - neither on the command nor the base scheme, and the
						// extension mechanism didn't fix this....so this is an error
						raiseExceptionWithHelp(helpRequested, helpOption, baseScheme, command,
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
			raiseExceptionWithHelp(helpRequested, helpOption, baseScheme, command,
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
			raiseExceptionWithHelp(helpRequested, helpOption, baseScheme, command,
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
			raiseExceptionWithHelp(helpRequested, helpOption, baseScheme, command,
				new InsufficientOptionValuesException(currentScheme, p));
		}

		// Validate that we have enough positionals as required
		if (currentScheme.getMinArguments() > positionals.size()) {
			raiseExceptionWithHelp(helpRequested, helpOption, baseScheme, command,
				new InsufficientPositionalValuesException(currentScheme));
		}

		// At this point, we have all the required options, all the options with minimum
		// values have been validated to be compliant, and we know we're not breaking the maximum
		// values limit because that's checked above as we process. Now we validate the positionals

		// This appears to be a schema-correct command line, so clean everything up and return the
		// result so it can be processed

		// Unwrap the positionals...
		List<String> positionalStrings = new ArrayList<>(positionals.size());
		positionals.stream().map(Token::getRawString).forEach(positionalStrings::add);

		raiseExceptionWithHelp(helpRequested, helpOption, baseScheme, command, null);
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
		if (InsufficientOptionValuesException.class.isInstance(e)) {
			throw InsufficientOptionValuesException.class.cast(e);
		}
		if (MissingRequiredCommandException.class.isInstance(e)) {
			throw MissingRequiredCommandException.class.cast(e);
		}
		if (MissingRequiredOptionsException.class.isInstance(e)) {
			throw MissingRequiredOptionsException.class.cast(e);
		}
		if (TooManyOptionValuesException.class.isInstance(e)) { throw TooManyOptionValuesException.class.cast(e); }
		if (TooManyPositionalValuesException.class.isInstance(e)) {
			throw TooManyPositionalValuesException.class.cast(e);
		}
		if (UnknownCommandException.class.isInstance(e)) { throw UnknownCommandException.class.cast(e); }
		if (UnknownOptionException.class.isInstance(e)) { throw UnknownOptionException.class.cast(e); }
	}
}