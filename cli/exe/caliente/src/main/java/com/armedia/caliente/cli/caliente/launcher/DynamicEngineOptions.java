package com.armedia.caliente.cli.caliente.launcher;

import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.caliente.command.CalienteCommand;

public interface DynamicEngineOptions {

	public void getDynamicOptions(CalienteCommand command, OptionScheme scheme);

}
