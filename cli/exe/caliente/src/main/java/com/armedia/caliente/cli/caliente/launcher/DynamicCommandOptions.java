package com.armedia.caliente.cli.caliente.launcher;

import com.armedia.caliente.cli.OptionScheme;

@FunctionalInterface
public interface DynamicCommandOptions {

	public void getDynamicOptions(String engine, OptionScheme scheme);

}
