package com.armedia.caliente.cli.datagen;

import java.util.Set;

import com.armedia.caliente.cli.parser.MutableParameter;
import com.armedia.caliente.cli.parser.Parameter;

public enum CLIParam implements Parameter {
	debug(new MutableParameter() //
		.setDescription("Enable increased logging for debugging") //
	), //
	target(new MutableParameter() //
		.setRequired(true) //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("folder or cabinet") //
		.setDescription("The root folder within which to create the data") //
	), //
	object_types(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(-1) //
		.setValueName("type1,type2,type3,...,typeN") //
		.setDescription("Names of the object types to generate samples for (must be subtypes of dm_sysobject)") //
	), //
	tree_depth(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("depth") //
		.setDescription("The depth of the tree (defaults to 1)") //
	), //
	folder_count(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("number") //
		.setDescription("The number of folders inside each folder (defaults to one per folder type included)") //
	), //
	document_count(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("number") //
		.setDescription("The number of documents inside each folder (defaults to one per non-folder type included)") //
	), //
	document_min_size(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("threads") //
		.setDescription(
			"The minimum size for the (random) content stream for each document (min = 1 byte). Suffixes such "
				+ "as KB and MB are supported - no suffix = bytes.") //
	), //
	document_max_size(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("threads") //
		.setDescription(
			"The maximum size for the (random) content stream for each document (capped out at 16MB). Suffixes such "
				+ "as KB and MB are supported - no suffix = bytes.") //
	), //
	name_format(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("threads") //
		.setDescription(
			"The format string to name files and folders after (supports ${type}, ${id}, ${number} and ${uuid})") //
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
	public boolean isEqual(Parameter other) {
		return this.parameter.isEqual(other);
	}
}