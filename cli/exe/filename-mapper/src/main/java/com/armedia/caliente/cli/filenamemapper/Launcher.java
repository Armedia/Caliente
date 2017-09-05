package com.armedia.caliente.cli.filenamemapper;

import java.util.Arrays;
import java.util.Collection;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionGroupImpl;
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

	private final LibLaunchHelper libLaunchHelper = new LibLaunchHelper();
	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionValues baseValues, String command,
		OptionValues commandValies, Collection<String> positionals) {
		return Arrays.asList(this.libLaunchHelper, this.dfcLaunchHelper);
	}

	@Override
	protected OptionScheme getOptionScheme() {
		return new OptionScheme(getProgramName()) //
			.addGroup( //
				new OptionGroupImpl("Library") //
					.add(this.libLaunchHelper) //
			) //
			.addGroup( //
				new OptionGroupImpl("Documentum") //
					.add(this.dfcLaunchHelper) //
			) //
			.add(Option.unwrap(CLIParam.values())) //
		;
	}

	@Override
	protected String getProgramName() {
		return "Caliente Filename Mapper and Deduplicator";
	}

	@Override
	protected int run(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws Exception {
		return new FilenameMapper(this.dfcLaunchHelper).run(baseValues);
	}
}