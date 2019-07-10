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
package com.armedia.caliente.cli.exception;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.token.Token;
import com.armedia.commons.utilities.Tools;

public class CommandLineExtensionException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private static final String NO_DETAILS = "(no details given)";

	private final int pass;
	private final OptionValues baseValues;
	private final String command;
	private final OptionValues commandValues;
	private final String details;

	public CommandLineExtensionException(int pass, OptionValues baseValues, String command, OptionValues commandValues,
		Token token, String details) {
		super(null, null, token);
		this.pass = pass;
		this.baseValues = baseValues;
		this.command = command;
		this.commandValues = commandValues;
		this.details = (StringUtils.isBlank(details) ? CommandLineExtensionException.NO_DETAILS : details);
	}

	public final int getPass() {
		return this.pass;
	}

	public final OptionValues getBaseValues() {
		return this.baseValues;
	}

	public final String getCommand() {
		return this.command;
	}

	public final OptionValues getCommandValues() {
		return this.commandValues;
	}

	public final String getDetails() {
		return this.details;
	}

	@Override
	protected String renderMessage() {
		String commandStr = " (no command)";
		if (this.command != null) {
			commandStr = String.format(" (command=[%s])", this.command);
		}
		return String.format("Failed to extend the parameter schema on token %s on pass #%d%s: %s", getToken(),
			this.pass, commandStr, Tools.coalesce(this.details, this.details));
	}
}