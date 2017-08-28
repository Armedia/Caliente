package com.armedia.caliente.cli.datagen;

import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionWrapper;

public enum CLIParam implements OptionWrapper {
	debug(new OptionImpl() //
		.setDescription("Enable increased logging for debugging") //
	), //
	target(new OptionImpl() //
		.setRequired(true) //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("folder or cabinet") //
		.setDescription("The root folder within which to create the data") //
	), //
	object_types(new OptionImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(-1) //
		.setValueName("type1,type2,type3,...,typeN") //
		.setDescription("Names of the object types to generate samples for (must be subtypes of dm_sysobject)") //
	), //
	tree_depth(new OptionImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("depth") //
		.setDescription("The depth of the tree (defaults to 1)") //
	), //
	folder_count(new OptionImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("number") //
		.setDescription("The number of folders inside each folder (defaults to one per folder type included)") //
	), //
	document_count(new OptionImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("number") //
		.setDescription("The number of documents inside each folder (defaults to one per non-folder type included)") //
	), //
	document_min_size(new OptionImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("size") //
		.setDescription(
			"The minimum size for the (random) content stream for each document (min = 1 byte). Suffixes such "
				+ "as KB and MB are supported - no suffix = bytes.") //
	), //
	document_max_size(new OptionImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("size") //
		.setDescription(
			"The maximum size for the (random) content stream for each document (capped out at 16MB). Suffixes such "
				+ "as KB and MB are supported - no suffix = bytes.") //
	), //
	name_format(new OptionImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("format") //
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
	public Option getOption() {
		return this.option;
	}
}