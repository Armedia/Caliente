package com.armedia.caliente.cli;

import java.util.List;

public interface ParameterResult {

	/**
	 * Returns the {@link ParameterValues} instance that describes the parameters parsed
	 *
	 * @return the {@link ParameterValues} instance that describes the parameters parsed
	 */

	public ParameterValues getParameterValues();

	/**
	 * Returns the list of positional parameter values (i.e. non-flags) issued at the end of the
	 * command line.
	 *
	 * @return the list of positional parameter values (i.e. non-flags) issued at the end of the
	 *         command line.
	 */
	public List<String> getPositionals();
}