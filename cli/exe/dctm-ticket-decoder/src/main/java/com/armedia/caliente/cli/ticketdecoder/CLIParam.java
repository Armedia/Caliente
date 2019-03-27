package com.armedia.caliente.cli.ticketdecoder;

import java.util.function.Supplier;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.filter.EnumValueFilter;

public enum CLIParam implements Supplier<Option> {
	//
	debug(
		new OptionImpl() //
			.setDescription("Enable increased logging for debugging") //
	), //

	content_filter(
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("filter-expression") //
			.setDescription(
				"A JEXL 3 expression that will be boiled down to TRUE (non-0 number, non-null result, etc) or FALSE (null result, 0-number) which will be used to select which object(s) are included in the output. The content object can be referenced as 'content'") //
	),
	//

	format( //
		new OptionImpl() //
			.setRequired(true) //
			.setArgumentLimits(1) //
			.setArgumentName("format") //
			.setDescription("The output format") //
			.setValueFilter(new EnumValueFilter<>(false, PersistenceFormat.class)) //
	), //

	from( //
		new OptionImpl() //
			.setRequired(true) //
			.setArgumentLimits(1, -1) //
			.setArgumentName("source-spec") //
			.setDescription(
				"The source specifications identifying which content to extract (%searchKey, @fileref, /path, or query string)") //
	), //

	rendition_filter(
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("filter-expression") //
			.setDescription(
				"A JEXL 3 expression that will be boiled down to TRUE (non-0 number, non-null result, etc) or FALSE (null result, 0-number) which will be used to select which rendition(s) are included in the output. The rendition object can be referenced as 'rendition'") //
	),
	//

	rendition_preference(
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setArgumentName("[type:]format[:modifier]") //
			.setValueSep(',') //
			.setDescription(
				"A (set of) text representations of the rendition to select.  Only the first rendition matching the given format and modifier will be selected, or the default stream if none matches") //
	), //

	target(
		new OptionImpl() //
			.setRequired(true) //
			.setArgumentLimits(1) //
			.setArgumentName("target-file") //
			.setDescription("The file to which output will be written") //
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