package com.armedia.caliente.cli;

public interface CommandSchemeResult extends ParameterSchemeResult {

	/**
	 * Get the command given
	 *
	 * @return the command given, or {@code null} if none was given.
	 */
	public Command getCommand();

	/**
	 * Return the {@link ParameterValues} instance associated with the given command
	 *
	 * @return the {@link ParameterValues} instance associated with the given command, or
	 *         {@code null} if none was given.
	 */
	public ParameterValues getCommandValues();

}