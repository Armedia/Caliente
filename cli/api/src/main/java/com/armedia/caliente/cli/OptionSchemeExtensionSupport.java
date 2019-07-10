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
package com.armedia.caliente.cli;

import com.armedia.caliente.cli.exception.CommandLineExtensionException;
import com.armedia.caliente.cli.token.Token;

@FunctionalInterface
public interface OptionSchemeExtensionSupport {

	/**
	 * <p>
	 * Extend {@code scheme} by adding any options that may be supported additionally from the base
	 * scheme. New options may not collide with existing ones with either short or long options.
	 * This will result in an exception being raised and parsing aborted.
	 * </p>
	 *
	 * @param currentNumber
	 *            The number of times the method has been invoked during the parsing
	 * @param baseValues
	 *            the values captured so far for the base options (immutable)
	 * @param currentCommand
	 *            the name of the command currently being processed, if any ({@code null} if none)
	 * @param commandValues
	 *            the values captured so far for the command's options (immutable, {@code null} if
	 *            no command is active)
	 * @param extender
	 *            The object through which to extend the underlying option scheme
	 *
	 */
	public void extendScheme(int currentNumber, OptionValues baseValues, String currentCommand,
		OptionValues commandValues, Token currentToken, OptionSchemeExtender extender)
		throws CommandLineExtensionException;

}