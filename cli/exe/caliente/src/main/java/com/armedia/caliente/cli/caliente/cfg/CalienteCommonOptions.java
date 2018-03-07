package com.armedia.caliente.cli.caliente.cfg;

import java.net.URL;
import java.util.Collection;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.Options;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.LibLaunchHelper;

public class CalienteCommonOptions extends Options implements LaunchClasspathHelper {

	public static final String DEFAULT_LOG_FORMAT = "caliente-${logEngine}-${logMode}-${logTimeStamp}";

	public static final Option HELP = new OptionImpl() //
		.setShortOpt('h') //
		.setLongOpt("help") //
		.setDescription("This help message");

	public static final Option LIB = LibLaunchHelper.LIB;

	public static final Option LOG = new OptionImpl() //
		.setArgumentLimits(1) //
		.setDescription("The base name of the log file to use (${logName}).") //
		.setDefault(CalienteCommonOptions.DEFAULT_LOG_FORMAT) //
		.setArgumentName("log-name-template");

	public static final Option LOG_CFG = new OptionImpl() //
		.setArgumentLimits(1) //
		.setDescription(
			"The Log4j configuration (XML format) to use instead of the default (can reference ${logName} from --log)") //
		.setArgumentName("configuration");

	private final LibLaunchHelper lib = new LibLaunchHelper();

	private final OptionGroup group;

	public CalienteCommonOptions() {
		this.group = new OptionGroupImpl("Base Options") //
			.add(CalienteCommonOptions.HELP) //
			.add(CalienteCommonOptions.LIB) //
			.add(CalienteCommonOptions.LOG) //
			.add(CalienteCommonOptions.LOG_CFG) //
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
