package com.armedia.caliente.cli;

import java.util.List;

import com.armedia.commons.utilities.Tools;

public final class OptionParseResult {

	private final OptionValues optionValues;
	private final String command;
	private final OptionValues commandValues;
	private final List<String> positionals;

	/**
	 * @param optionValues
	 * @param command
	 * @param commandValues
	 * @param positionals
	 */
	OptionParseResult(OptionValues optionValues, String command, OptionValues commandValues, List<String> positionals) {
		if (optionValues == null) {
			throw new IllegalArgumentException("Must provide the option values for the base options - even if empty");
		}
		this.optionValues = optionValues;
		if ((command != null) && (commandValues != null)) {
			this.command = command;
			this.commandValues = commandValues;
		} else if ((command == null) && (commandValues == null)) {
			this.command = null;
			this.commandValues = null;
		} else {
			throw new IllegalArgumentException("Both command and commandValues must be null, or both must be non-null");
		}
		this.positionals = Tools.freezeList(positionals, true);
	}

	/**
	 * Returns the {@link OptionValues} instance that describes the options parsed
	 *
	 * @return the {@link OptionValues} instance that describes the options parsed
	 */
	public OptionValues getOptionValues() {
		return this.optionValues;
	}

	/**
	 * Returns {@code true} if a command was given, {@code false} otherwise. If this method returns
	 * {@code true}, both {@link #getCommand()} and {@link #getCommandValues()} will return
	 * non-{@code null} values. Otherwise, they will both return {@code null} values.
	 *
	 * @return {@code true} if a command was given, {@code false} otherwise.
	 */
	public boolean hasCommand() {
		return this.command != null;
	}

	/**
	 * Get the command given
	 *
	 * @return the command given, or {@code null} if none was given.
	 */
	public String getCommand() {
		return this.command;
	}

	/**
	 * Return the {@link OptionValues} instance associated with the given command
	 *
	 * @return the {@link OptionValues} instance associated with the given command, or {@code null}
	 *         if none was given.
	 */
	public OptionValues getCommandValues() {
		return this.commandValues;
	}

	/**
	 * Returns the list of positional option values (i.e. non-flags) issued at the end of the
	 * command line.
	 *
	 * @return the list of positional option values (i.e. non-flags) issued at the end of the
	 *         command line.
	 */
	public List<String> getPositionals() {
		return this.positionals;
	}
}