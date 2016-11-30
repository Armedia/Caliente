package com.armedia.caliente.cli.datagen;

import java.util.Arrays;
import java.util.Collection;

import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.launcher.LaunchParameterSet;
import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.Parameter;
import com.armedia.caliente.cli.parser.ParameterTools;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsHelper;

public class Launcher extends AbstractLauncher implements LaunchParameterSet {
	protected static final int MIN_THREADS = 1;
	protected static final int DEFAULT_THREADS = (Runtime.getRuntime().availableProcessors() / 2);
	protected static final int MAX_THREADS = (Runtime.getRuntime().availableProcessors());

	private final ThreadsHelper threadsParameter = new ThreadsHelper(Launcher.MIN_THREADS,
		Launcher.DEFAULT_THREADS, Launcher.MAX_THREADS);
	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);

	@Override
	public Collection<? extends Parameter> getParameters(CommandLineValues commandLine) {
		return ParameterTools.getUnwrappedList(CLIParam.values());
	}

	@Override
	protected Collection<? extends LaunchParameterSet> getLaunchParameterSets(CommandLineValues cli, int pass) {
		if (pass > 0) { return null; }
		return Arrays.asList(this, this.dfcLaunchHelper, this.threadsParameter);
	}

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(CommandLineValues cli) {
		return Arrays.asList(this.dfcLaunchHelper);
	}

	@Override
	protected String getProgramName(int pass) {
		return "Caliente Data Generator";
	}

	@Override
	protected int run(CommandLineValues cli) throws Exception {
		return new DataGen(this.threadsParameter, this.dfcLaunchHelper).run(cli);
	}
}