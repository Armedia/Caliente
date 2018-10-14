package com.armedia.caliente.cli.caliente.launcher;

import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.caliente.command.CommandModule;

public interface DynamicEngineOptions {

	public void getDynamicOptions(CommandModule<?> command, OptionScheme scheme);

}
