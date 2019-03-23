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

	renditions(
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setArgumentName("rendition-number") //
			.setDescription(
				"Exclude the folder in the count (defaults to ALL except these, may be specified multiple times) - path or object ID is valid") //
	)
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