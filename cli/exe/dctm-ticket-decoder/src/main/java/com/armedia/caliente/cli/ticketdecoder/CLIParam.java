/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
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

	log( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("log-name-template") //
			.setDefault(CLIConst.DEFAULT_LOG_FORMAT) //
			.setDescription("The base name of the log file to use (${logName}).") //
	), //

	log_config( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("configuration") //
			.setDescription(
				"The Log4j configuration (XML format) to use instead of the default (can reference ${logName} from --log)") //
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