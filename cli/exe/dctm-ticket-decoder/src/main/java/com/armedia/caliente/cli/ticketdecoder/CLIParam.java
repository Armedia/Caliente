package com.armedia.caliente.cli.ticketdecoder;

import java.util.function.Supplier;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;

public enum CLIParam implements Supplier<Option> {
	//
	debug(
		new OptionImpl() //
			.setDescription("Enable increased logging for debugging") //
	), //

	content_filter(
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("filter-expression") //
			.setDescription(
				"A JEXL 3 expression that will be boiled down to TRUE (non-0 number, non-null result, etc) or FALSE (null result, 0-number) which will be used to select which object(s) are included in the output. The following variables can be used in the script: id, path, renditions") //
	),
	//

	rendition_filter(
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("filter-expression") //
			.setDescription(
				"A JEXL 3 expression that will be boiled down to TRUE (non-0 number, non-null result, etc) or FALSE (null result, 0-number) which will be used to select which rendition(s) are included in the output. The following variables can be used in the script: number, page, format, modifier") //
	),
	//

	target(
		new OptionImpl() //
			.setRequired(true) //
			.setArgumentLimits(1) //
			.setArgumentName("target-file") //
			.setDescription("The file to which output will be written") //
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