package com.armedia.caliente.cli.caliente.cfg;

import java.net.URL;
import java.util.Collection;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.Options;
import com.armedia.caliente.cli.StringValueFilter;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.LibLaunchHelper;

public class CalienteBaseOptions extends Options implements LaunchClasspathHelper {

	public static final String DEFAULT_LOG_FORMAT = "caliente-${logEngine}-${logMode}-${logTimeStamp}";

	public static final Option HELP = new OptionImpl() //
		.setShortOpt('h') //
		.setLongOpt("help") //
		.setDescription("This help message") //
	;

	public static final Option LIB = LibLaunchHelper.LIB;

	public static final Option LOG = new OptionImpl() //
		.setArgumentLimits(1) //
		.setDescription("The base name of the log file to use (${logName}).") //
		.setDefault(CalienteBaseOptions.DEFAULT_LOG_FORMAT) //
		.setArgumentName("log-name-template") //
	;

	public static final Option LOG_CFG = new OptionImpl() //
		.setArgumentLimits(1) //
		.setDescription(
			"The Log4j configuration (XML format) to use instead of the default (can reference ${logName} from --log)") //
		.setArgumentName("configuration") //
	;

	public static final Option ENGINE = new OptionImpl() //
		.setShortOpt('e') //
		.setArgumentLimits(1) //
		.setValueFilter( // TODO: Find a way to make this list dynamic
			new StringValueFilter( //
				false, // Case-insensitive
				"dctm", //
				"alfresco", //
				"cmis", //
				"sharepoint", //
				"ucm", //
				"xml", //
				"local" //
			) //
		) //
		.setRequired(true) //
		.setDescription("The mode of operation") //
		.setArgumentName("engine") //
	;

	public static final Option DB = new OptionImpl() //
		.setShortOpt('d') //
		.setArgumentLimits(1) //
		.setArgumentName("metadata-directory-or-config") //
		.setRequired(true) //
		.setDescription(
			"The directory into which the metadata database will be stored, or the XML file that describes the store configuration") //
		.setDefault("caliente") //
	;

	public static final Option CONTENT = new OptionImpl() //
		.setShortOpt('c') //
		.setArgumentLimits(1) //
		.setArgumentName("content-directory-or-config") //
		.setRequired(true) //
		.setDescription(
			"The directory into which the content streams will be stored (if omitted, it will be placed in the 'content' subdirectory of the Database directory), or the XML file that describes the store configuration") //
	;

	private final LibLaunchHelper lib = new LibLaunchHelper();

	private final OptionGroup group;

	public CalienteBaseOptions() {
		this.group = new OptionGroupImpl("Base Options") //
			.add(CalienteBaseOptions.HELP) //
			.add(CalienteBaseOptions.LIB) //
			.add(CalienteBaseOptions.LOG) //
			.add(CalienteBaseOptions.LOG_CFG) //
			.add(CalienteBaseOptions.ENGINE) //
			.add(CalienteBaseOptions.DB) //
			.add(CalienteBaseOptions.CONTENT) //
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