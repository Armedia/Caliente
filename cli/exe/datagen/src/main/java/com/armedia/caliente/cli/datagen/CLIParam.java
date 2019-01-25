package com.armedia.caliente.cli.datagen;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionWrapper;

public enum CLIParam implements OptionWrapper {
	debug(new OptionImpl() //
		.setDescription("Enable increased logging for debugging") //
	), //
	target(new OptionImpl() //
		.setRequired(true) //
		.setMinArguments(1) //
		.setMaxArguments(1) //
		.setArgumentName("folder-or-cabinet") //
		.setDescription("The root folder within which to create the data") //
	), //
	object_types(new OptionImpl() //
		.setMinArguments(1) //
		.setMaxArguments(-1) //
		.setArgumentName("type") //
		.setDescription("Names of the object types to generate samples for (must be subtypes of dm_sysobject)") //
	), //
	tree_depth(new OptionImpl() //
		.setMinArguments(1) //
		.setMaxArguments(1) //
		.setArgumentName("depth") //
		.setDescription("The depth of the tree") //
		.setDefault("1") //
	), //
	folder_count(new OptionImpl() //
		.setMinArguments(1) //
		.setMaxArguments(1) //
		.setArgumentName("number") //
		.setDescription("The number of folders inside each folder per folder type included") //
		.setDefault("1") //
	), //
	document_count(new OptionImpl() //
		.setMinArguments(1) //
		.setMaxArguments(1) //
		.setArgumentName("number") //
		.setDescription("The number of non-folder objects (documents) inside each folder per non-folder type included") //
		.setDefault("1") //
	), //
	document_min_size(new OptionImpl() //
		.setMinArguments(1) //
		.setMaxArguments(1) //
		.setArgumentName("size") //
		.setDescription(
			"The minimum size for the (random) content stream for each document (min = 1 byte). Suffixes such "
				+ "as KB and MB are supported - no suffix = bytes.") //
	), //
	document_max_size(new OptionImpl() //
		.setMinArguments(1) //
		.setMaxArguments(1) //
		.setArgumentName("size") //
		.setDescription(
			"The maximum size for the (random) content stream for each document (capped out at 16MB). Suffixes such "
				+ "as KB and MB are supported - no suffix = bytes.") //
	), //
	name_format(new OptionImpl() //
		.setMinArguments(1) //
		.setMaxArguments(1) //
		.setArgumentName("format") //
		.setDescription(
			"The format string to name files and folders after (supports ${type}, ${id}, ${number} and ${uuid})") //
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