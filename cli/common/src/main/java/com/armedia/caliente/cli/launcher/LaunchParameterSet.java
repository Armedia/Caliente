package com.armedia.caliente.cli.launcher;

import java.util.Collection;

import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.Parameter;

public interface LaunchParameterSet {

	/**
	 * <p>
	 * Returns the parameter definitions to be applied when parsing the command line.
	 * </p>
	 *
	 * @param commandLine
	 * @return the collection of {@link Parameter} instances to use in parsing the command line
	 */
	public Collection<? extends Parameter> getParameters(CommandLineValues commandLine);

}