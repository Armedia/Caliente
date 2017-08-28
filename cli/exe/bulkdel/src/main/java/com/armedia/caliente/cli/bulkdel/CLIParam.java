package com.armedia.caliente.cli.bulkdel;

import java.util.Arrays;

import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionWrapper;

public enum CLIParam implements OptionWrapper {
	debug(new OptionImpl() //
		.setDescription("Enable increased logging for debugging") //
	), //
	abort_on_error(new OptionImpl() //
		.setDescription("Instead of logging and ignoring errors, abort the operation") //
	), //
	recursive(new OptionImpl() //
		.setDescription("Delete all folder children, recursively") //
	), //
	delete_all_children(new OptionImpl() //
		.setDescription("Delete folder children that may also reside in folders that aren't part of the deletion set") //
	), //
	delete_vdoc_children(new OptionImpl() //
		.setDescription("Delete vdoc children that may reside in folders that aren't part of the deletion set") //
	), //
	delete_referenced(new OptionImpl() //
		.setDescription(
			"If any references are encountered, delete the referenced object instead of the reference itself (default is to delete just the reference)") //
	), //
	delete_versions(new OptionImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("mode") //
		.setAllowedValues(Arrays.asList("selected", "unused", "all"))
		.setDescription("Select which versions of the objects to delete (default = all)") //
	), //
	answer(new OptionImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("answer") //
		.setAllowedValues(Arrays.asList("yes", "no", "abort", "ask"))
		.setDescription("How to answer any yes-no questions posed during the operation (default = abort)") //
	), //
	target(new OptionImpl() //
		.setMinValueCount(0) //
		.setMaxValueCount(-1) //
		.setValueName("target") //
		.setValueSep(null) // Disallow concatenated values
		.setDescription("The path or r_object_id of each object to be deleted (can be specified multiple times)") //
	), //
	predicate(new OptionImpl() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("DQL-predicate") //
		.setDescription(
			"A DQL predicate that will be used to retrieve the r_object_id for all the objects that need to be deleted") //
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