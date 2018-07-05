package com.armedia.caliente.cli.caliente.options;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.Options;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;

public class CalienteCommonOptions extends Options {

	public static final Option THREADS = ThreadsLaunchHelper.THREADS;

	public static final Option TRANSFORMATIONS = new OptionImpl() //
		.setArgumentName("transformations-file") //
		.setArgumentLimits(1) //
		.setDescription("The object transformations descriptor file") //
	;

	public static final Option FILTERS = new OptionImpl() //
		.setArgumentName("filters-file") //
		.setArgumentLimits(1) //
		.setDescription("The object filters descriptor file") //
	;

	public static final Option EXTERNAL_METADATA = new OptionImpl() //
		.setArgumentName("external-metadata-file") //
		.setArgumentLimits(1) //
		.setDescription("The external metadata descriptor file") //
	;

	private final OptionGroup group;

	public CalienteCommonOptions() {
		this.group = new OptionGroupImpl("Common Engine Options") //
			.add(CalienteCommonOptions.THREADS) //
			.add(CalienteCommonOptions.TRANSFORMATIONS) //
			.add(CalienteCommonOptions.FILTERS) //
			.add(CalienteCommonOptions.EXTERNAL_METADATA) //
		;
	}

	@Override
	public OptionGroup asGroup(String name) {
		return this.group;
	}

}
