package com.armedia.caliente.cli.filenamemapper;

import com.armedia.caliente.cli.ParameterImpl;
import com.armedia.caliente.cli.Parameter;
import com.armedia.caliente.cli.ParameterWrapper;

public enum CLIParam implements ParameterWrapper {
	no_fix(new ParameterImpl() //
		.setDescription("Disable filename fixes") //
	), //
	no_length_fix(new ParameterImpl() //
		.setDescription("Disable length repairs on the filename fixer") //
	), //
	no_char_fix(new ParameterImpl() //
		.setDescription("Disable invalid character repairs on the filename fixer") //
	), //
	ignore_case(new ParameterImpl() //
		.setDescription("Disable case sensitivity when performing name comparisons") //
	), //
	fix_char(new ParameterImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("character") //
		.setDescription("Use the given character as the replacement for illegal characters (default is '_', "
			+ "must not be a forbidden character in the target fix scheme, and the period ('.') "
			+ "and spaces are not allowed in Windows)") //
	), //
	fix_mode(new ParameterImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("WIN|UNIX") //
		.setDescription("Filename fix mode. Valid values are WIN (Windows compatibility) or "
			+ "UNIX (Unix compatibility) - defaults to the current platform") //
	), //
	no_dedup(new ParameterImpl() //
		.setDescription("Disable filename deduplication") //
	), //
	dedup_pattern(new ParameterImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("pattern") //
		.setDescription("The Deduplication pattern to apply - must contain ${id}, and can contain any of ${name}, "
			+ "${fixChar}, and ${count} (the number of conflicts resolved so far) - default is "
			+ "\"${name}${fixChar}${id}\"") //
	), //
	target(new ParameterImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("file") //
		.setDescription("The target file to write the properties into (default filenamemap.xml)") //
	), //
		//
	;

	private final Parameter parameter;

	private CLIParam(ParameterImpl parameter) {
		this.parameter = ParameterImpl.initOptionName(this, parameter);
	}

	@Override
	public Parameter getParameter() {
		return this.parameter;
	}
}