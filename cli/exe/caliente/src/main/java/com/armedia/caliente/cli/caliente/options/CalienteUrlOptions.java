package com.armedia.caliente.cli.caliente.options;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.Options;

public class CalienteUrlOptions extends Options {

	public static final Option URL = new OptionImpl() //
		.setArgumentName("url") //
		.setArgumentLimits(1) //
		.setRequired(true) //
		.setDescription("The server URL for the connection") //
	;

	public static final Option USER = new OptionImpl() //
		.setArgumentName("user") //
		.setArgumentLimits(1) //
		.setDescription("The user to authenticate as") //
	;

	public static final Option PASSWORD = new OptionImpl() //
		.setArgumentName("password") //
		.setArgumentLimits(1) //
		.setDescription("The password to authenticate with") //
	;

	public static final Option DOMAIN = new OptionImpl() //
		.setArgumentName("domain") //
		.setArgumentLimits(1) //
		.setDescription("The domain the user should authenticate against") //
	;

	private final OptionGroup group;

	public CalienteUrlOptions() {
		this.group = new OptionGroupImpl("Common URL Options") //
			.add(CalienteUrlOptions.URL) //
			.add(CalienteUrlOptions.USER) //
			.add(CalienteUrlOptions.PASSWORD) //
			.add(CalienteUrlOptions.DOMAIN) //
		;
	}

	@Override
	public OptionGroup asGroup(String name) {
		return this.group;
	}

}