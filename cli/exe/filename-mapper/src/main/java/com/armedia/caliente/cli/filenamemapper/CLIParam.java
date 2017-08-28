package com.armedia.caliente.cli.filenamemapper;

import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionWrapper;

public enum CLIParam implements OptionWrapper {
	no_fix(new OptionImpl() //
		.setDescription("Disable filename fixes") //
	), //
	no_length_fix(new OptionImpl() //
		.setDescription("Disable length repairs on the filename fixer") //
	), //
	no_char_fix(new OptionImpl() //
		.setDescription("Disable invalid character repairs on the filename fixer") //
	), //
	ignore_case(new OptionImpl() //
		.setDescription("Disable case sensitivity when performing name comparisons") //
	), //
	fix_char(new OptionImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("character") //
		.setDescription("Use the given character as the replacement for illegal characters (default is '_', "
			+ "must not be a forbidden character in the target fix scheme, and the period ('.') "
			+ "and spaces are not allowed in Windows)") //
	), //
	fix_mode(new OptionImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("WIN|UNIX") //
		.setDescription("Filename fix mode. Valid values are WIN (Windows compatibility) or "
			+ "UNIX (Unix compatibility) - defaults to the current platform") //
	), //
	no_dedup(new OptionImpl() //
		.setDescription("Disable filename deduplication") //
	), //
	dedup_pattern(new OptionImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("pattern") //
		.setDescription("The Deduplication pattern to apply - must contain ${id}, and can contain any of ${name}, "
			+ "${fixChar}, and ${count} (the number of conflicts resolved so far) - default is "
			+ "\"${name}${fixChar}${id}\"") //
	), //
	target(new OptionImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("file") //
		.setDescription("The target file to write the properties into (default filenamemap.xml)") //
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