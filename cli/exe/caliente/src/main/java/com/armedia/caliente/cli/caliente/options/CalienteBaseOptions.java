package com.armedia.caliente.cli.caliente.options;

import java.net.URL;
import java.util.Collection;

import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.Options;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.LibLaunchHelper;

public class CalienteBaseOptions extends Options implements LaunchClasspathHelper {

	public static final String DEFAULT_LOG_FORMAT = "caliente-${logEngine}-${logMode}-${logTimeStamp}";

	private final LibLaunchHelper lib = new LibLaunchHelper();

	private final OptionGroup group;

	public CalienteBaseOptions() {
		this.group = new OptionGroupImpl("Base Options") //
			.add(CLIOptions.HELP) //
			.add(CLIOptions.LIB) //
			.add(CLIOptions.LOG) //
			.add(CLIOptions.LOG_CFG) //
			.add(CLIOptions.ENGINE) //
		;
	}

	@Override
	public Collection<URL> getClasspathPatchesPre(OptionValues values) {
		return this.lib.getClasspathPatchesPre(values);
	}

	@Override
	public Collection<URL> getClasspathPatchesPost(OptionValues values) {
		return this.lib.getClasspathPatchesPost(values);
	}

	@Override
	public OptionGroup asGroup(String name) {
		return this.group;
	}

}
