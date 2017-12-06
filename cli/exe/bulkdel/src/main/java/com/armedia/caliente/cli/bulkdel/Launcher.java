package com.armedia.caliente.cli.bulkdel;

import java.util.Arrays;
import java.util.Collection;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.LibLaunchHelper;

public class Launcher extends AbstractLauncher {

	public static final void main(String... args) {
		System.exit(new Launcher().launch(args));
	}

	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);
	private final LibLaunchHelper libLaunchHelper = new LibLaunchHelper();

	private Launcher() {
	}

	@Override
	protected OptionScheme getOptionScheme() {
		return new OptionScheme(getProgramName()) //
			.add( //
				this.libLaunchHelper.asGroup() //
			) //
			.add( //
				this.dfcLaunchHelper.asGroup() //
			) //
			.add( //
				Option.unwrap(CLIParam.values()) //
		) //
		;
	}

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionValues baseValues, String command,
		OptionValues commandValies, Collection<String> positionals) {
		return Arrays.asList(this.libLaunchHelper, this.dfcLaunchHelper);
	}

	@Override
	protected String getProgramName() {
		return "caliente-bulk-deleter";
	}

	@Override
	protected int run(OptionValues baseValues, String command, OptionValues commandValies,
		Collection<String> positionals) throws Exception {
		return new BulkDel(this.dfcLaunchHelper).run(baseValues);
	}
}