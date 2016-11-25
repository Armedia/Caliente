package com.armedia.caliente.cli.validator;

import java.util.List;
import java.util.Set;

import com.armedia.caliente.cli.parser.MutableParameter;
import com.armedia.caliente.cli.parser.Parameter;

public enum CLIParam implements Parameter {
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
	public final int getMinValueCount() {
		return this.parameter.getMinValueCount();
	}

	@Override
	public final int getMaxValueCount() {
		return this.parameter.getMaxValueCount();
	}

	@Override
	public Set<String> getAllowedValues() {
		return this.parameter.getAllowedValues();
	}

	@Override
	public String getDefault() {
		return this.parameter.getDefault();
	}

	@Override
	public List<String> getDefaults() {
		return this.parameter.getDefaults();
	}
}