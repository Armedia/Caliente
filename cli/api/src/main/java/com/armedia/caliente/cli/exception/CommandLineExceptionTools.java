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

public class CommandLineExceptionTools {

	/**
	 * Unwinds the given throwable into one of the 7 subclasses of
	 * {@link CommandLineSyntaxException}. If the given throwable is either {@code null}, or not one
	 * of these classes, this method does nothing and execution continues normally. This is useful
	 * in the event that closer analysis of a {@link CommandLineSyntaxException} is desired. The API
	 * is designed to make it easy to catch these exceptions and process them simply, but if more
	 * detail is required, this method is useful.
	 *
	 * @param t
	 * @throws UnknownOptionException
	 * @throws UnknownCommandException
	 * @throws TooManyPositionalValuesException
	 * @throws TooManyOptionValuesException
	 * @throws InsufficientPositionalValuesException
	 * @throws InsufficientOptionValuesException
	 * @throws MissingRequiredOptionsException
	 * @throws MissingRequiredCommandException
	 */
	public static void unwindSyntaxException(final Throwable t) throws UnknownOptionException, UnknownCommandException,
		TooManyPositionalValuesException, TooManyOptionValuesException, InsufficientPositionalValuesException,
		InsufficientOptionValuesException, MissingRequiredOptionsException, MissingRequiredCommandException {
		if (t == null) { return; }
		if (UnknownOptionException.class.isInstance(t)) { throw UnknownOptionException.class.cast(t); }
		if (UnknownCommandException.class.isInstance(t)) { throw UnknownCommandException.class.cast(t); }
		if (TooManyPositionalValuesException.class.isInstance(t)) {
			throw TooManyPositionalValuesException.class.cast(t);
		}
		if (TooManyOptionValuesException.class.isInstance(t)) { throw TooManyOptionValuesException.class.cast(t); }
		if (InsufficientPositionalValuesException.class.isInstance(t)) {
			throw InsufficientPositionalValuesException.class.cast(t);
		}
		if (InsufficientOptionValuesException.class.isInstance(t)) {
			throw InsufficientOptionValuesException.class.cast(t);
		}
		if (MissingRequiredOptionsException.class.isInstance(t)) {
			throw MissingRequiredOptionsException.class.cast(t);
		}
		if (MissingRequiredCommandException.class.isInstance(t)) {
			throw MissingRequiredCommandException.class.cast(t);
		}
	}

}