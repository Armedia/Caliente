package com.armedia.caliente.cli.filenamemapper;

import java.util.Set;

import com.armedia.caliente.cli.parser.MutableParameter;
import com.armedia.caliente.cli.parser.Parameter;

public enum CLIParam implements Parameter {
	no_fix(new MutableParameter() //
		.setDescription("Disable filename fixes") //
	), //
	no_length_fix(new MutableParameter() //
		.setDescription("Disable length repairs on the filename fixer") //
	), //
	no_char_fix(new MutableParameter() //
		.setDescription("Disable invalid character repairs on the filename fixer") //
	), //
	ignore_case(new MutableParameter() //
		.setDescription("Disable case sensitivity when performing name comparisons") //
	), //
	fix_char(new MutableParameter() //
		.setDescription("Use the given character as the replacement for illegal characters (default is '_', "
			+ "must not be a forbidden character in the target fix scheme, and the period ('.') "
			+ "and spaces are not allowed in Windows)") //
	), //
	fix_mode(new MutableParameter() //
		.setDescription("Filename fix mode. Valid values are WIN (Windows compatibility) or "
			+ "UNIX (Unix compatibility) - defaults to the current platform") //
	), //
	no_dedup(new MutableParameter() //
		.setDescription("Disable filename deduplication") //
	), //
	dedup_pattern(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("pattern") //
		.setDescription("The Deduplication pattern to apply - must contain ${id}, and can contain any of ${name}, "
			+ "${fixChar}, and ${count} (the number of conflicts resolved so far) - default is "
			+ "\"${name}${fixChar}${id}\"") //
	), //
	target(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("file") //
		.setDescription("The target file to write the properties into (default filenamemap.xml)") //
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
}