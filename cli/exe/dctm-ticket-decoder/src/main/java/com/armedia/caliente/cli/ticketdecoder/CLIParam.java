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

	prefer_rendition(
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setArgumentName("[type:]format[:modifier]") //
			.setValueSep(',') //
			.setDescription( //
				"A (set of) text representations of rendition preference.  Only the first " + //
					"rendition matching the given preference specifier will be selected, " + //
					"or the default stream if there are no better matches.  The syntax is " + //
					"[type:][format%modifier][@age] where type is any combination of the " + //
					"digits [0123] and matches Documentum's rendition types, format can be " + //
					"any format name or the * to indicate that any format may do, %modifier " + //
					"is any modifier string or the special values - and + indicating the " + //
					"lowest and highest values (sorted alphabetically), and @age can be " + //
					"@old/@oldest or @new/@newest to indicate the oldest or newest value judged by the " + //
					"rendition's set_time attribute." //
			) //
	), //

	rendition_filter(
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("filter-expression") //
			.setDescription(
				"A JEXL 3 expression that will be boiled down to TRUE (non-0 number, non-null result, etc) or FALSE (null result, 0-number) which will be used to select which rendition(s) are included in the output. The rendition object can be referenced as 'rendition'") //
	),
	//

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