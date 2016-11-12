package com.armedia.caliente.cli.filenamemapper;

import com.armedia.caliente.cli.parser.MutableParameterDefinition;
import com.armedia.caliente.cli.parser.ParameterDefinition;

public enum CLIParam implements ParameterDefinition {
	//
	lib(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("directory") //
		.setDescription(
			"The directory which contains extra classes (JARs, ZIPs or a classes directory) that should be added to the classpath") //
	), //
	no_fix(new MutableParameterDefinition() //
		.setValueCount(0) //
		.setDescription("Disable filename fixes") //
	), //
	no_length_fix(new MutableParameterDefinition() //
		.setValueCount(0) //
		.setDescription("Disable length repairs on the filename fixer") //
	), //
	no_char_fix(new MutableParameterDefinition() //
		.setValueCount(0) //
		.setDescription("Disable invalid character repairs on the filename fixer") //
	), //
	ignore_case(new MutableParameterDefinition() //
		.setValueCount(0) //
		.setDescription("Disable case sensitivity when performing name comparisons") //
	), //
	fix_char(new MutableParameterDefinition() //
		.setValueCount(0) //
		.setDescription("Use the given character as the replacement for illegal characters (default is '_', "
			+ "must not be a forbidden character in the target fix scheme, and the period ('.') "
			+ "and spaces are not allowed in Windows)") //
	), //
	fix_mode(new MutableParameterDefinition() //
		.setValueCount(0) //
		.setDescription("Filename fix mode. Valid values are WIN (Windows compatibility) or "
			+ "UNIX (Unix compatibility) - defaults to the current platform") //
	), //
	no_dedup(new MutableParameterDefinition() //
		.setValueCount(0) //
		.setDescription("Disable filename deduplication") //
	), //
	dedup_pattern(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("pattern") //
		.setDescription("The Deduplication pattern to apply - must contain ${id}, and can contain any of ${name}, "
			+ "${fixChar}, and ${count} (the number of conflicts resolved so far) - default is "
			+ "\"${name}${fixChar}${id}\"") //
	), //
	target(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("file") //
		.setDescription("The target file to write the properties into (default filenamemap.xml)") //
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