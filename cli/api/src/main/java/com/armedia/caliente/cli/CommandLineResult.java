package com.armedia.caliente.cli;

import java.util.List;

import com.armedia.commons.utilities.Tools;

public final class CommandLineResult {

	private final ParameterValues parameterValues;
	private final String command;
	private final ParameterValues commandValues;
	private final List<String> positionals;

	/**
	 * @param parameterValues
	 * @param command
	 * @param commandValues
	 * @param positionals
	 */
	public CommandLineResult(ParameterValues parameterValues, String command, ParameterValues commandValues,
		List<String> positionals) {
		if (parameterValues == null) { throw new IllegalArgumentException(
			"Must provide the parameter values for the base parameters - even if empty"); }
		this.parameterValues = parameterValues;
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
	 * Returns the {@link ParameterValues} instance that describes the parameters parsed
	 *
	 * @return the {@link ParameterValues} instance that describes the parameters parsed
	 */
	public ParameterValues getParameterValues() {
		return this.parameterValues;
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
	 * Return the {@link ParameterValues} instance associated with the given command
	 *
	 * @return the {@link ParameterValues} instance associated with the given command, or
	 *         {@code null} if none was given.
	 */
	public ParameterValues getCommandValues() {
		return this.commandValues;
	}

	/**
	 * Returns the list of positional parameter values (i.e. non-flags) issued at the end of the
	 * command line.
	 *
	 * @return the list of positional parameter values (i.e. non-flags) issued at the end of the
	 *         command line.
	 */
	public List<String> getPositionals() {
		return this.positionals;
	}
}