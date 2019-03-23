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