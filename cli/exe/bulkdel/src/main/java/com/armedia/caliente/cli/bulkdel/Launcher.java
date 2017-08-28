package com.armedia.caliente.cli.bulkdel;

import java.util.Arrays;
import java.util.Collection;

import com.armedia.caliente.cli.CommandLineValues;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.launcher.LaunchOptionSet;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;

public class Launcher extends AbstractLauncher implements LaunchOptionSet {

	public static final void main(String... args) {
		System.exit(new Launcher().launch(args));
	}

	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);

	@Override
	public Collection<? extends Option> getParameters(CommandLineValues commandLine) {
		return Option.getUnwrappedList(CLIParam.values());
	}

	@Override
	protected Collection<? extends LaunchOptionSet> getLaunchParameterSets(CommandLineValues cli, int pass) {
		if (pass > 0) { return null; }
		return Arrays.asList(this, this.dfcLaunchHelper);
	}

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(CommandLineValues cli) {
		return Arrays.asList(this.dfcLaunchHelper);
	}

	@Override
	protected String getProgramName(int pass) {
		return "Caliente Bulk Deleter";
	}

	@Override
	protected int run(CommandLineValues cli) throws Exception {
		return new BulkDel(this.dfcLaunchHelper).run(cli);
	}
}