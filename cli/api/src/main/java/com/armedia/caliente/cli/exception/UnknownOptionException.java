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

import com.armedia.caliente.cli.Command;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.token.Token;
import com.armedia.commons.utilities.Tools;

public class UnknownOptionException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	public UnknownOptionException(OptionScheme scheme, Token token) {
		super(scheme, null, token);
	}

	@Override
	protected String renderMessage() {
		String commandPart = "";
		Command command = Tools.cast(Command.class, getOptionScheme());
		if (command != null) {
			commandPart = String.format(" as part of the '%s' command", command.getName());
		}
		return String.format("The option [%s] is not recognized%s", getToken().getRawString(), commandPart);
	}
}