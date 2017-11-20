package com.armedia.caliente.tools;

import java.util.Arrays;
import java.util.Collection;

import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.commons.utilities.FileNameTools;

/**
 * This class is used as a testbed to run quick'n'dirty DFC test programs
 *
 * @author diego.rivera@armedia.com
 *
 */
public class Scratchpad extends AbstractLauncher {

	public static final void main(String... args) {
		System.exit(new Scratchpad().launch(args));
	}

	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionValues baseValues, String command,
		OptionValues commandValies, Collection<String> positionals) {
		return Arrays.asList(this.dfcLaunchHelper);
	}

	@Override
	protected String getProgramName() {
		return "Caliente Scratchpad";
	}

	@Override
	protected int run(OptionValues baseValues, String command, OptionValues commandValues,
		Collection<String> positionals) throws Exception {
		String path = "(unfiled)/some/weird/path/name";
		while (path != null) {
			System.out.printf("PATH=[%s]%n", path);
			path = FileNameTools.dirname(path);
			if (path.equals(".")) {
				break;
			}
		}
		// PropertiesTest.test();
		// DctmTest.test();
		return 0;
	}

	@Override
	protected OptionScheme getOptionScheme() {
		return null;
		/*
					return new OptionScheme(getProgramName()) //
					.add( //
					this.dfcLaunchHelper.asGroup() //
					) //
					;
					*/
	}
}