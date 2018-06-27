package com.armedia.caliente.cli.caliente.cfg;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.Options;

public class CalienteExportOptions extends Options {

	public static final Option DIRECT_FS = new OptionImpl() //
		.setDescription("Export files to local FS duplicating the CMS's path") //
	;

	public static final Option SOURCE = new OptionImpl() //
		.setArgumentLimits(1) //
		.setRequired(true) //
		.setArgumentName("source-spec") //
		.setDescription("The source specification identifying which content to extract") //
	;

	private final OptionGroup group;

	public CalienteExportOptions() {
		this.group = new OptionGroupImpl("Common Export Options") //
			.add(CalienteExportOptions.DIRECT_FS) //
			.add(CalienteExportOptions.SOURCE) //
		;
	}

	@Override
	public OptionGroup asGroup(String name) {
		return this.group;
	}

}