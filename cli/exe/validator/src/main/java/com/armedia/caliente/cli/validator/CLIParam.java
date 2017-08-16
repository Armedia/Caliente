package com.armedia.caliente.cli.validator;

import com.armedia.caliente.cli.MutableParameter;
import com.armedia.caliente.cli.Parameter;
import com.armedia.caliente.cli.ParameterWrapper;

public enum CLIParam implements ParameterWrapper {
	bulk_import(new MutableParameter() //
		.setRequired(true) //
		.setShortOpt('i') //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("bulk import directory") //
		.setDescription("The location of the Bulk Import source data") //
	), //
	bulk_export(new MutableParameter() //
		.setRequired(true) //
		.setShortOpt('e') //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("bulk export directory") //
		.setDescription("The location of the Bulk Export validation data") //
	), //
	report_dir(new MutableParameter() //
		.setShortOpt('r') //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("directory") //
		.setDescription("The directory where the validation reports will be output to") //
	), //
	model(new MutableParameter() //
		.setRequired(true) //
		.setShortOpt('m') //
		.setMinValueCount(1) //
		.setMaxValueCount(-1) //
		.setValueName("model1,model2,model3,...,modelN") //
		.setDescription("The (list of) content model(s) in XML format and proper dependency order") //
	), //
		//
	;

	private final Parameter parameter;

	private CLIParam(MutableParameter parameter) {
		this.parameter = MutableParameter.initOptionName(this, parameter).freezeCopy();
	}

	@Override
	public Parameter getParameter() {
		return this.parameter;
	}
}