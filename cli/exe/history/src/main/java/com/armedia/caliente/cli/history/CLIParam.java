package com.armedia.caliente.cli.history;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionWrapper;

public enum CLIParam implements OptionWrapper {
	debug(new OptionImpl() //
		.setDescription("Enable increased logging for debugging") //
	), //
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