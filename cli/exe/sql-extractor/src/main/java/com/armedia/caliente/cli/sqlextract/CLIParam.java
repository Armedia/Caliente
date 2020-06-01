/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.cli.sqlextract;

import java.util.function.Supplier;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.filter.EnumValueFilter;

public enum CLIParam implements Supplier<Option> {
	//
	condition(
		new OptionImpl() //
			.setRequired(true) //
			.setArgumentLimits(1) //
			.setArgumentName("where-clause") //
			.setDescription(
				"The SQL 'where' clause to be applied to the primary table in order to select the records to be extracted") //
	), //

	debug(
		new OptionImpl() //
			.setDescription("Enable increased logging for debugging") //
	), //

	format( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("format") //
			.setDescription("The output format") //
			.setValueFilter(new EnumValueFilter<>(false, ResultsFormat.class)) //
			.setDefault(ResultsFormat.XML) //
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
				"The SLF4J configuration (XML format) to use instead of the default (can reference ${logName} from --log)") //
	), //

	password(
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("password") //
			.setDescription("The password for the JDBC user") //
	), //

	properties(
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("properties-file") //
			.setDescription("A Properties file with JDBC properties to be applied") //
			.setDefault("sql-extraction.properties")), //

	table(
		new OptionImpl() //
			.setRequired(true) //
			.setArgumentLimits(1) //
			.setArgumentName("table-name") //
			.setDescription("The name of the table that will serve as a starting point for the crawl") //
	), //

	target(
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("target-file") //
			.setDescription("The file to which output will be written") //
	), //

	url(
		new OptionImpl() //
			.setRequired(true) //
			.setArgumentLimits(1) //
			.setArgumentName("jdbc-url") //
			.setDescription("The JDBC URL to use for connections") //
	), //

	user(
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("username") //
			.setDescription("The JDBC username to authenticate with") //
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