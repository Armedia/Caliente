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
package com.armedia.caliente.cli.filenamemapper;

import java.util.function.Supplier;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.filter.StringValueFilter;

public enum CLIParam implements Supplier<Option> {
	no_fix(
		new OptionImpl() //
			.setDescription("Disable filename fixes") //
	), //
	no_length_fix(
		new OptionImpl() //
			.setDescription("Disable length repairs on the filename fixer") //
	), //
	no_char_fix(
		new OptionImpl() //
			.setDescription("Disable invalid character repairs on the filename fixer") //
	), //
	ignore_case(
		new OptionImpl() //
			.setDescription("Disable case sensitivity when performing name comparisons") //
	), //
	fix_char(
		new OptionImpl() //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("character") //
			.setDescription("Use the given character as the replacement for illegal characters (default is '_', "
				+ "must not be a forbidden character in the target fix scheme, and the period ('.') "
				+ "and spaces are not allowed in Windows)") //
			.setDefault("_") //
	), //
	fix_mode(
		new OptionImpl() //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("WIN") //
			.setValueFilter(new StringValueFilter("WIN", "LIN")) //
			.setDescription("Filename fix mode. Defaults to the current platform") //
	), //
	no_dedup(
		new OptionImpl() //
			.setDescription("Disable filename deduplication") //
	), //
	dedup_pattern(
		new OptionImpl() //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("pattern") //
			.setDescription("The Deduplication pattern to apply - must contain ${id}, and can contain any of ${name}, "
				+ "${fixChar}, and ${count} (the number of conflicts resolved so far) - default is "
				+ "\"${name}${fixChar}${id}\"") //
			.setDefault("${name}${fixChar}${id}") //
	), //
	target(
		new OptionImpl() //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("file") //
			.setDescription("The target file to write the properties into (default filenamemap.xml)") //
			.setDefault("filenamemap.xml") //
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