package com.armedia.caliente.cli.bulkdel;

import java.util.Arrays;

import com.armedia.caliente.cli.ParameterImpl;
import com.armedia.caliente.cli.Parameter;
import com.armedia.caliente.cli.ParameterWrapper;

public enum CLIParam implements ParameterWrapper {
	debug(new ParameterImpl() //
		.setDescription("Enable increased logging for debugging") //
	), //
	abort_on_error(new ParameterImpl() //
		.setDescription("Instead of logging and ignoring errors, abort the operation") //
	), //
	recursive(new ParameterImpl() //
		.setDescription("Delete all folder children, recursively") //
	), //
	delete_all_children(new ParameterImpl() //
		.setDescription("Delete folder children that may also reside in folders that aren't part of the deletion set") //
	), //
	delete_vdoc_children(new ParameterImpl() //
		.setDescription("Delete vdoc children that may reside in folders that aren't part of the deletion set") //
	), //
	delete_referenced(new ParameterImpl() //
		.setDescription(
			"If any references are encountered, delete the referenced object instead of the reference itself (default is to delete just the reference)") //
	), //
	delete_versions(new ParameterImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("mode") //
		.setAllowedValues(Arrays.asList("selected", "unused", "all"))
		.setDescription("Select which versions of the objects to delete (default = all)") //
	), //
	answer(new ParameterImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("answer") //
		.setAllowedValues(Arrays.asList("yes", "no", "abort", "ask"))
		.setDescription("How to answer any yes-no questions posed during the operation (default = abort)") //
	), //
	target(new ParameterImpl() //
		.setMinValueCount(0) //
		.setMaxValueCount(-1) //
		.setValueName("target") //
		.setValueSep(null) // Disallow concatenated values
		.setDescription("The path or r_object_id of each object to be deleted (can be specified multiple times)") //
	), //
	predicate(new ParameterImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("DQL-predicate") //
		.setDescription(
			"A DQL predicate that will be used to retrieve the r_object_id for all the objects that need to be deleted") //
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