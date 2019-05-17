package com.armedia.caliente.cli.flat2db;

import java.util.function.Supplier;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;

public enum CLIParam implements Supplier<Option> {
	cfg(
		new OptionImpl() //
			.setRequired(true) //
			.setShortOpt('c') //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("configuration") //
			.setDescription("The configuration file") //
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