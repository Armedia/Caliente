package com.armedia.caliente.cli.datagen;

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
	debug(new MutableParameterDefinition() //
		.setDescription("Enable increased logging for debugging") //
	), //
	threads(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("threads") //
		.setDescription("The number of threads to use for document generation") //
	), //
	target(new MutableParameterDefinition() //
		.setRequired(true) //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("folder or cabinet") //
		.setDescription("The root folder within which to create the data") //
	), //
	object_types(new MutableParameterDefinition() //
		.setValueCount(-1) //
		.setValueOptional(false) //
		.setValueName("type1,type2,type3,...,typeN") //
		.setDescription("Names of the object types to generate samples for (must be subtypes of dm_sysobject)") //
	), //
	tree_depth(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("depth") //
		.setDescription("The depth of the tree (defaults to 1)") //
	), //
	folder_count(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("number") //
		.setDescription("The number of folders inside each folder (defaults to one per folder type included)") //
	), //
	document_count(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("number") //
		.setDescription("The number of documents inside each folder (defaults to one per non-folder type included)") //
	), //
	document_min_size(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("threads") //
		.setDescription(
			"The minimum size for the (random) content stream for each document (min = 1 byte). Suffixes such "
				+ "as KB and MB are supported - no suffix = bytes.") //
	), //
	document_max_size(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("threads") //
		.setDescription(
			"The maximum size for the (random) content stream for each document (capped out at 16MB). Suffixes such "
				+ "as KB and MB are supported - no suffix = bytes.") //
	), //
	name_format(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("threads") //
		.setDescription(
			"The format string to name files and folders after (supports ${type}, ${id}, ${number} and ${uuid})") //
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