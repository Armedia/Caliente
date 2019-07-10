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
package com.armedia.caliente.cli.validator;

import java.util.function.Supplier;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;

public enum CLIParam implements Supplier<Option> {
	bulk_import(
		new OptionImpl() //
			.setRequired(true) //
			.setShortOpt('i') //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("bulk-import-directory") //
			.setDescription("The location of the Bulk Import source data") //
	), //
	bulk_export(
		new OptionImpl() //
			.setRequired(true) //
			.setShortOpt('e') //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("bulk-export-directory") //
			.setDescription("The location of the Bulk Export validation data") //
	), //
	report_dir(
		new OptionImpl() //
			.setShortOpt('r') //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("directory") //
			.setDescription("The directory where the validation reports will be output to") //
	), //
	model(
		new OptionImpl() //
			.setRequired(true) //
			.setShortOpt('m') //
			.setMinArguments(1) //
			.setMaxArguments(-1) //
			.setArgumentName("model") //
			.setDescription("The (list of) content model(s) in XML format and proper dependency order") //
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