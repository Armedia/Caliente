package com.armedia.caliente.cli.datagen;

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

	protected static final int MIN_THREADS = 1;
	protected static final int DEFAULT_THREADS = (Runtime.getRuntime().availableProcessors() / 2);
	protected static final int MAX_THREADS = (Runtime.getRuntime().availableProcessors());

	private final ThreadsLaunchHelper threadsParameter = new ThreadsLaunchHelper(Launcher.MIN_THREADS,
		Launcher.DEFAULT_THREADS, Launcher.MAX_THREADS);
	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);

	@Override
	protected Collection<? extends LaunchClasspathHelper> getClasspathHelpers(OptionValues baseValues, String command,
		OptionValues commandValies, Collection<String> positionals) {
		return Arrays.asList(this.dfcLaunchHelper);
	}

	@Override
	protected String getProgramName() {
		return "Caliente Data Generator";
	}

	@Override
	protected OptionScheme getOptionScheme() {
		OptionScheme optionScheme = new OptionScheme(getProgramName());
		for (Option o : Option.getUnwrappedList(CLIParam.values())) {
			optionScheme.add(o);
		}
		for (Option o : this.dfcLaunchHelper.getOptions()) {
			optionScheme.add(o);
		}
		for (Option o : this.threadsParameter.getOptions()) {
			optionScheme.add(o);
		}
		return optionScheme;
	}

	@Override
	protected int run(OptionValues baseValues, String command, OptionValues commandValies,
		Collection<String> positionals) throws Exception {
		return new DataGen(this.threadsParameter, this.dfcLaunchHelper).run(baseValues);
	}
}