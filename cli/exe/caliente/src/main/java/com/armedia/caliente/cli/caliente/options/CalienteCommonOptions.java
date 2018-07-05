package com.armedia.caliente.cli.caliente.options;

import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.Options;

public class CalienteCommonOptions extends Options {

	private final OptionGroup group;

	public CalienteCommonOptions() {
		this.group = new OptionGroupImpl("Common Engine Options") //
			.add(CLIOptions.THREADS) //
			.add(CLIOptions.NO_RENDITIONS) //
			.add(CLIOptions.NO_VERSIONS) //
			.add(CLIOptions.SKIP_CONTENT) //
			.add(CLIOptions.EXCLUDE_TYPES) //
			.add(CLIOptions.TRANSFORMATIONS) //
			.add(CLIOptions.FILTERS) //
			.add(CLIOptions.EXTERNAL_METADATA) //
		;
	}

	@Override
	public OptionGroup asGroup(String name) {
		return this.group;
	}

}
