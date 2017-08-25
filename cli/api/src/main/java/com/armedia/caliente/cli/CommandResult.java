package com.armedia.caliente.cli;

public interface CommandResult extends ParameterResult {

	/**
	 * Get the command given
	 *
	 * @return the command given, or {@code null} if none was given.
	 */
	public String getCommand();

	/**
	 * Return the {@link ParameterValues} instance associated with the given command
	 *
	 * @return the {@link ParameterValues} instance associated with the given command, or
	 *         {@code null} if none was given.
	 */
	public ParameterValues getCommandValues();

}