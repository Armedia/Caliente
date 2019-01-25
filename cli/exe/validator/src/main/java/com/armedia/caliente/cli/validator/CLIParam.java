package com.armedia.caliente.cli.validator;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionWrapper;

public enum CLIParam implements OptionWrapper {
	bulk_import(new OptionImpl() //
		.setRequired(true) //
		.setShortOpt('i') //
		.setMinArguments(1) //
		.setMaxArguments(1) //
		.setArgumentName("bulk-import-directory") //
		.setDescription("The location of the Bulk Import source data") //
	), //
	bulk_export(new OptionImpl() //
		.setRequired(true) //
		.setShortOpt('e') //
		.setMinArguments(1) //
		.setMaxArguments(1) //
		.setArgumentName("bulk-export-directory") //
		.setDescription("The location of the Bulk Export validation data") //
	), //
	report_dir(new OptionImpl() //
		.setShortOpt('r') //
		.setMinArguments(1) //
		.setMaxArguments(1) //
		.setArgumentName("directory") //
		.setDescription("The directory where the validation reports will be output to") //
	), //
	model(new OptionImpl() //
		.setRequired(true) //
		.setShortOpt('m') //
		.setMinArguments(1) //
		.setMaxArguments(-1) //
		.setArgumentName("model") //
		.setDescription("The (list of) content model(s) in XML format and proper dependency order") //
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