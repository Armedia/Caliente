package com.armedia.caliente.cli.bulkdel;

import java.util.function.Supplier;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.StringValueFilter;

public enum CLIParam implements Supplier<Option> {
	debug(
		new OptionImpl() //
			.setDescription("Enable increased logging for debugging") //
	), //
	abort_on_error(
		new OptionImpl() //
			.setDescription("Instead of logging and ignoring errors, abort the operation") //
	), //
	recursive(
		new OptionImpl() //
			.setDescription("Delete all folder children, recursively") //
	), //
	delete_all_children(
		new OptionImpl() //
			.setDescription(
				"Delete folder children that may also reside in folders that aren't part of the deletion set") //
	), //
	delete_vdoc_children(
		new OptionImpl() //
			.setDescription("Delete vdoc children that may reside in folders that aren't part of the deletion set") //
	), //
	delete_referenced(
		new OptionImpl() //
			.setDescription(
				"If any references are encountered, delete the referenced object instead of the reference itself (default is to delete just the reference)") //
	), //
	delete_versions(
		new OptionImpl() //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("mode") //
			.setValueFilter(new StringValueFilter("selected", "unused", "all"))
			.setDescription("Select which versions of the objects to delete (default = all)") //
			.setDefault("all") //
	), //
	answer(
		new OptionImpl() //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("answer") //
			.setValueFilter(new StringValueFilter("yes", "no", "abort", "ask"))
			.setDescription("How to answer any yes-no questions posed during the operation") //
			.setDefault("abort") //
	), //
	target(
		new OptionImpl() //
			.setMinArguments(0) //
			.setMaxArguments(-1) //
			.setArgumentName("target") //
			.setValueSep(null) // Disallow concatenated values
			.setDescription("The path or r_object_id of each object to be deleted (can be specified multiple times)") //
	), //
	predicate(
		new OptionImpl() //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("DQL-predicate") //
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
	public Option get() {
		return this.option;
	}
}