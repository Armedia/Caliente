package com.armedia.caliente.cli.launcher;

import java.util.Collection;

import com.armedia.caliente.cli.CommandLineValues;
import com.armedia.caliente.cli.ParameterDefinition;

public interface LaunchParameterSet {

	/**
	 * <p>
	 * Returns the parameter definitions to be applied when parsing the command line.
	 * </p>
	 *
	 * @param commandLine
	 * @return the collection of {@link ParameterDefinition} instances to use in parsing the command line
	 */
	public Collection<? extends ParameterDefinition> getParameters(CommandLineValues commandLine);

}