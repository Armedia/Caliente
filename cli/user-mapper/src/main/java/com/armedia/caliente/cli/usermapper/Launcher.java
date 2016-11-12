package com.armedia.caliente.cli.usermapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.armedia.caliente.cli.launcher.AbstractDfcEnabledLauncher;
import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.ParameterDefinition;

public class Launcher extends AbstractDfcEnabledLauncher {

	public static final void main(String... args) {
		System.exit(new Launcher().launch(args));
	}

	@Override
	protected Collection<? extends ParameterDefinition> getCommandLineParameters(CommandLineValues commandLine,
		int pass) {
		if (pass > 0) { return null; }
		List<ParameterDefinition> ret = new ArrayList<>();
		ret.addAll(getDfcParameters());
		ret.addAll(Arrays.asList(CLIParam.values()));
		return ret;
	}

	@Override
	protected int processCommandLine(CommandLineValues commandLine) {
		return super.processCommandLine(commandLine);
	}

	@Override
	protected String getProgramName(int pass) {
		return "Caliente User Mapper";
	}

	@Override
	protected int run(CommandLineValues cli) throws Exception {
		return new UserMapper(cli).run();
	}
}