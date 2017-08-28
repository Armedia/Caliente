package com.armedia.caliente.cli.usermapper;

import java.util.Arrays;
import java.util.Collection;

import com.armedia.caliente.cli.CommandLineValues;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.launcher.LaunchOptionSet;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.LibLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;

public class Launcher extends AbstractLauncher implements LaunchOptionSet {

	public static final void main(String... args) {
		System.exit(new Launcher().launch(args));
	}

	private final ThreadsLaunchHelper threadsParameter = new ThreadsLaunchHelper();
	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);
	private final LibLaunchHelper libLaunchHelper = new LibLaunchHelper();

	@Override
	protected Collection<? extends LaunchOptionSet> getLaunchParameterSets(CommandLineValues cli, int pass) {
		if (pass > 0) { return null; }
		return Arrays.asList(this, this.libLaunchHelper, this.dfcLaunchHelper, this.threadsParameter);
	}

	@Override
	public Collection<Option> getParameters(CommandLineValues commandLine) {
		return Option.getUnwrappedList(CLIParam.values());
	}

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(CommandLineValues cli) {
		return Arrays.asList(this.libLaunchHelper, this.dfcLaunchHelper);
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
	protected int run(final CommandLineValues cli) throws Exception {
		return new UserMapper(this.dfcLaunchHelper).run(cli);
	}
}