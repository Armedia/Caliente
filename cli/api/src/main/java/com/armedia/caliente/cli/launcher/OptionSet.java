package com.armedia.caliente.cli.launcher;

import java.util.Collection;

import com.armedia.caliente.cli.Option;

public interface OptionSet {

	/**
	 * <p>
	 * Returns the option definitions to be applied when parsing the command line.
	 * </p>
	 *
	 * @return the collection of {@link Option} instances to use in parsing the command line
	 */
	public Collection<? extends Option> getOptions();

}