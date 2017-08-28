package com.armedia.caliente.tools;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.armedia.caliente.cli.CommandLineValues;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.launcher.LaunchOptionSet;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;

/**
 * This class is used as a testbed to run quick'n'dirty DFC test programs
 *
 * @author diego.rivera@armedia.com
 *
 */
public class Scratchpad extends AbstractLauncher implements LaunchOptionSet {

	public static final void main(String... args) {
		System.exit(new Scratchpad().launch(args));
	}

	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);

	@Override
	public Collection<? extends Option> getParameters(CommandLineValues commandLine) {
		return Collections.emptyList();
	}

	@Override
	protected Collection<? extends LaunchOptionSet> getLaunchParameterSets(CommandLineValues cli, int pass) {
		return null;
	}

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(CommandLineValues cli) {
		return Arrays.asList(this.dfcLaunchHelper);
	}

	@Override
	protected String getProgramName(int pass) {
		return "Caliente Scratchpad";
	}

	@Override
	protected int run(CommandLineValues cli) throws Exception {
		// PropertiesTest.test();
		// DctmTest.test();
		return 0;
	}
}