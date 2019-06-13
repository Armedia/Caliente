package com.armedia.caliente.tools;

import java.util.function.Supplier;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;

public enum CLIParam implements Supplier<Option> {
	//
	debug(
		new OptionImpl() //
			.setDescription("Enable increased logging for debugging") //
	), //

	log( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("log-name-template") //
			.setDefault(CLIConst.DEFAULT_LOG_FORMAT) //
			.setDescription("The base name of the log file to use (${logName}).") //
	), //

	log_config( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("configuration") //
			.setDescription(
				"The Log4j configuration (XML format) to use instead of the default (can reference ${logName} from --log)") //
	), //

	//
	;

	private final Option option;

	private CLIParam(OptionImpl parameter) {
		this.option = OptionImpl.initOptionName(this, parameter);
	}

	@Override
	public Option get() {
		return this.option;
	}
}