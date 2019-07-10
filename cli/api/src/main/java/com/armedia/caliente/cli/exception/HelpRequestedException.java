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
package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;

public class HelpRequestedException extends CommandLineException {
	private static final long serialVersionUID = 1L;

	private final Option helpOption;
	private final OptionScheme baseScheme;
	private final Command command;

	public HelpRequestedException(Option helpOption, OptionScheme baseScheme) {
		this(helpOption, baseScheme, null, null);
	}

	public HelpRequestedException(Option helpOption, OptionScheme baseScheme, Command command) {
		this(helpOption, baseScheme, command, null);
	}

	public HelpRequestedException(Option helpOption, OptionScheme baseScheme, CommandLineSyntaxException error) {
		this(helpOption, baseScheme, null, error);
	}

	public HelpRequestedException(Option helpOption, OptionScheme baseScheme, Command command,
		CommandLineSyntaxException error) {
		super(error);
		this.helpOption = helpOption;
		this.baseScheme = baseScheme;
		this.command = command;
	}

	@Override
	public CommandLineSyntaxException getCause() {
		return CommandLineSyntaxException.class.cast(super.getCause());
	}

	public Option getHelpOption() {
		return this.helpOption;
	}

	public OptionScheme getBaseScheme() {
		return this.baseScheme;
	}

	public Command getCommand() {
		return this.command;
	}
}