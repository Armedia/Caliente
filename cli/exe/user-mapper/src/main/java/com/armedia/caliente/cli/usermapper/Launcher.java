package com.armedia.caliente.cli.usermapper;

import java.util.Collection;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.LibLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;

public class Launcher extends AbstractLauncher {

	public static final void main(String... args) {
		System.exit(new Launcher().launch(args));
	}

	private final ThreadsLaunchHelper threadsParameter = new ThreadsLaunchHelper();
	private final DfcLaunchHelper dfcLaunchHelper = new DfcLaunchHelper(true);
	private final LibLaunchHelper libLaunchHelper = new LibLaunchHelper();

	@Override
	protected String getProgramName() {
		return "Caliente User Mapper";
	}

	@Override
	protected OptionScheme getOptionScheme() {
		OptionScheme optionScheme = new OptionScheme(getProgramName());
		for (Option o : Option.getUnwrappedList(CLIParam.values())) {
			optionScheme.add(o);
		}
		for (Option o : this.libLaunchHelper.getOptions()) {
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
		return new UserMapper(this.dfcLaunchHelper).run(baseValues);
	}
}