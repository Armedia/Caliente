package com.armedia.caliente.cli.caliente.options;

import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.Options;

public class CalienteMailOptions extends Options {

	private final OptionGroup group;

	public CalienteMailOptions() {
		this.group = new OptionGroupImpl("Caliente SMTP Options") //
			.add(CLIOptions.MAIL_FROM) //
			.add(CLIOptions.MAIL_TO) //
			.add(CLIOptions.MAIL_CC) //
			.add(CLIOptions.MAIL_BCC) //
			.add(CLIOptions.MAIL_HOST) //
			.add(CLIOptions.MAIL_PORT) //
			.add(CLIOptions.MAIL_SSL) //
			.add(CLIOptions.MAIL_USER) //
			.add(CLIOptions.MAIL_PASSWORD) //
		// .add(CLIOptions.MAIL_AUTH) //
		;
	}

	@Override
	public OptionGroup asGroup(String name) {
		return this.group;
	}

}