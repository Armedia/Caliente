package com.armedia.caliente.cli.bulkdel;

import java.util.Arrays;
import java.util.Collection;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;

public class Launcher extends AbstractLauncher {

	public static final void main(String... args) {
		System.exit(new Launcher().launch(args));
	}

	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);

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
		return new BulkDel(this.dfcLaunchHelper).run(baseValues);
	}
}