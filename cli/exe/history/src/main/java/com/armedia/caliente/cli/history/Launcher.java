package com.armedia.caliente.cli.history;

import java.util.Arrays;
import java.util.Collection;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;

public class Launcher extends AbstractLauncher {

	public static final void main(String... args) {
		System.exit(new Launcher().launch(args));
	}

	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);
	private final ThreadsLaunchHelper threadsLaunchHelper = new ThreadsLaunchHelper();

	private Launcher() {
	}

	@Override
	protected OptionScheme getOptionScheme() {
		OptionScheme optionScheme = new OptionScheme(getProgramName());
		for (Option o : Option.getUnwrappedList(CLIParam.values())) {
			optionScheme.addOrReplace(o);
		}
		for (Option o : this.dfcLaunchHelper.getOptions()) {
			optionScheme.addOrReplace(o);
		}
		optionScheme.setMinArgs(1);
		return optionScheme;
	}

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionValues baseValues, String command,
		OptionValues commandValies, Collection<String> positionals) {
		return Arrays.asList(this.dfcLaunchHelper);
	}

	@Override
	protected String getProgramName() {
		return "Caliente Bulk Deleter";
	}

	@Override
	protected int run(OptionValues baseValues, String command, OptionValues commandValies,
		Collection<String> positionals) throws Exception {
		return new History(this.dfcLaunchHelper, this.threadsLaunchHelper).run(baseValues, positionals);
	}
}