package com.armedia.caliente.cli.validator;

import com.armedia.caliente.cli.parser.MutableParameterDefinition;
import com.armedia.caliente.cli.parser.ParameterDefinition;

public enum CLIParam implements ParameterDefinition {
	bulk_import(new MutableParameterDefinition() //
		.setRequired(true) //
		.setShortOpt('i') //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("bulk import directory") //
		.setDescription("The location of the Bulk Import source data") //
	), //
	bulk_export(new MutableParameterDefinition() //
		.setRequired(true) //
		.setShortOpt('e') //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("bulk export directory") //
		.setDescription("The location of the Bulk Export validation data") //
	), //
	report_dir(new MutableParameterDefinition() //
		.setShortOpt('r') //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("directory") //
		.setDescription("The directory where the validation reports will be output to") //
	), //
	model(new MutableParameterDefinition() //
		.setRequired(true) //
		.setShortOpt('m') //
		.setValueCount(-1) //
		.setValueOptional(false) //
		.setValueName("model1,model2,model3,...,modelN") //
		.setDescription("The (list of) content model(s) in XML format and proper dependency order") //
	), //
		//
	;

	private final ParameterDefinition parameter;

	private CLIParam(MutableParameterDefinition parameter) {
		String name = name();
		if (name.length() == 1) {
			// If we decide that the name of the option will be a single character, we use that
			parameter.setShortOpt(name.charAt(0));
		} else if (parameter.getLongOpt() == null) {
			// Otherwise, use the name replacing underscores with dashes
			parameter.setLongOpt(name().replace('_', '-'));
		}
		this.parameter = parameter.clone();
	}

	@Override
	public final String getKey() {
		return this.parameter.getKey();
	}

	@Override
	public final boolean isRequired() {
		return this.parameter.isRequired();
	}

	@Override
	public final String getDescription() {
		return this.parameter.getDescription();
	}

	@Override
	public final String getLongOpt() {
		return this.parameter.getLongOpt();
	}

	@Override
	public final Character getShortOpt() {
		return this.parameter.getShortOpt();
	}

	@Override
	public final Character getValueSep() {
		return this.parameter.getValueSep();
	}

	@Override
	public final String getValueName() {
		return this.parameter.getValueName();
	}

	@Override
	public final int getValueCount() {
		return this.parameter.getValueCount();
	}

	@Override
	public final boolean isValueOptional() {
		return this.parameter.isValueOptional();
	}
}