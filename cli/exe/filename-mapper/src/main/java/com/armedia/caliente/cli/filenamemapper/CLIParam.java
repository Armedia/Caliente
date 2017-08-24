package com.armedia.caliente.cli.filenamemapper;

import com.armedia.caliente.cli.Parameter;
import com.armedia.caliente.cli.ParameterDefinition;
import com.armedia.caliente.cli.ParameterWrapper;

public enum CLIParam implements ParameterWrapper {
	no_fix(new Parameter() //
		.setDescription("Disable filename fixes") //
	), //
	no_length_fix(new Parameter() //
		.setDescription("Disable length repairs on the filename fixer") //
	), //
	no_char_fix(new Parameter() //
		.setDescription("Disable invalid character repairs on the filename fixer") //
	), //
	ignore_case(new Parameter() //
		.setDescription("Disable case sensitivity when performing name comparisons") //
	), //
	fix_char(new Parameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("character") //
		.setDescription("Use the given character as the replacement for illegal characters (default is '_', "
			+ "must not be a forbidden character in the target fix scheme, and the period ('.') "
			+ "and spaces are not allowed in Windows)") //
	), //
	fix_mode(new Parameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("WIN|UNIX") //
		.setDescription("Filename fix mode. Valid values are WIN (Windows compatibility) or "
			+ "UNIX (Unix compatibility) - defaults to the current platform") //
	), //
	no_dedup(new Parameter() //
		.setDescription("Disable filename deduplication") //
	), //
	dedup_pattern(new Parameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("pattern") //
		.setDescription("The Deduplication pattern to apply - must contain ${id}, and can contain any of ${name}, "
			+ "${fixChar}, and ${count} (the number of conflicts resolved so far) - default is "
			+ "\"${name}${fixChar}${id}\"") //
	), //
	target(new Parameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("file") //
		.setDescription("The target file to write the properties into (default filenamemap.xml)") //
	), //
		//
	;

	private final ParameterDefinition parameterDefinition;

	private CLIParam(Parameter parameter) {
		this.parameterDefinition = Parameter.initOptionName(this, parameter);
	}

	@Override
	public ParameterDefinition getParameter() {
		return this.parameterDefinition;
	}
}