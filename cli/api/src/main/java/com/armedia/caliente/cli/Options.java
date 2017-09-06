package com.armedia.caliente.cli;

import java.util.Collection;

public interface Options {

	/**
	 * <p>
	 * Returns the option definitions to be applied when parsing the command line.
	 * </p>
	 *
	 * @return the collection of {@link Option} instances to use in parsing the command line
	 */
	public Collection<? extends Option> getOptions();

}