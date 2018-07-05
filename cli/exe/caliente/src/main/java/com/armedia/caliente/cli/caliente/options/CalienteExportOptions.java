package com.armedia.caliente.cli.caliente.options;

import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.Options;

public class CalienteExportOptions extends Options {

	private final OptionGroup group;

	public CalienteExportOptions() {
		this.group = new OptionGroupImpl("Common Export Options") //
			.add(CLIOptions.DIRECT_FS) //
			.add(CLIOptions.SOURCE) //
		;
	}

	@Override
	public OptionGroup asGroup(String name) {
		return this.group;
	}

}