package com.armedia.caliente.cli.validator;

import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionWrapper;

public enum CLIParam implements OptionWrapper {
	bulk_import(new OptionImpl() //
		.setRequired(true) //
		.setShortOpt('i') //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("bulk import directory") //
		.setDescription("The location of the Bulk Import source data") //
	), //
	bulk_export(new OptionImpl() //
		.setRequired(true) //
		.setShortOpt('e') //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("bulk export directory") //
		.setDescription("The location of the Bulk Export validation data") //
	), //
	report_dir(new OptionImpl() //
		.setShortOpt('r') //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("directory") //
		.setDescription("The directory where the validation reports will be output to") //
	), //
	model(new OptionImpl() //
		.setRequired(true) //
		.setShortOpt('m') //
		.setMinValueCount(1) //
		.setMaxValueCount(-1) //
		.setValueName("model1,model2,model3,...,modelN") //
		.setDescription("The (list of) content model(s) in XML format and proper dependency order") //
	), //
		//
	;

	private final Option option;

	private CLIParam(OptionImpl parameter) {
		this.option = OptionImpl.initOptionName(this, parameter);
	}

	@Override
	public Option getOption() {
		return this.option;
	}
}