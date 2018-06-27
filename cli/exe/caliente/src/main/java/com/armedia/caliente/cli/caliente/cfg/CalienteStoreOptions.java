package com.armedia.caliente.cli.caliente.cfg;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.Options;

public class CalienteStoreOptions extends Options {

	public static final Option DB = new OptionImpl() //
		.setShortOpt('d') //
		.setArgumentLimits(1) //
		.setArgumentName("metadata-directory-or-config") //
		.setRequired(true) //
		.setDescription(
			"The directory into which the metadata database will be stored, or the XML file that describes the store configuration") //
		.setDefault("caliente") //
	;

	public static final Option CONTENT = new OptionImpl() //
		.setShortOpt('c') //
		.setArgumentLimits(1) //
		.setArgumentName("content-directory-or-config") //
		.setRequired(true) //
		.setDescription(
			"The directory into which the content streams will be stored (if omitted, it will be placed in the 'content' subdirectory of the Database directory), or the XML file that describes the store configuration") //
	;

	public static final Option CONTENT_STRATEGY = new OptionImpl() //
		.setShortOpt('o') //
		.setArgumentLimits(1) //
		.setArgumentName("organization") //
		.setDescription(
			"The name of the organization strategy to use in the Content directory (specific engines may override with their own defaults if they require it)") //
	;

	private final OptionGroup group;

	public CalienteStoreOptions() {
		this.group = new OptionGroupImpl("Data Store Options") //
			.add(CalienteStoreOptions.DB) //
			.add(CalienteStoreOptions.CONTENT) //
			.add(CalienteStoreOptions.CONTENT_STRATEGY) //
		;
	}

	@Override
	public OptionGroup asGroup(String name) {
		return this.group;
	}

}