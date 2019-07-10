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

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.token.Token;

public class TooManyOptionValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	public TooManyOptionValuesException(OptionScheme scheme, Option option, Token token) {
		super(scheme, option, token);
	}

	@Override
	protected String renderMessage() {
		int min = getOption().getMinArguments();
		int max = getOption().getMaxArguments();

		String msg = "";
		if (min == max) {
			if (max == 0) {
				msg = "no";
			} else {
				msg = String.format("exactly %d", max);
			}
		} else {
			msg = String.format("at most %d", max);
		}

		Option o = getOption();
		String longOpt = o.getLongOpt();
		Character shortOpt = o.getShortOpt();
		String option = "";
		if ((longOpt != null) && (shortOpt != null)) {
			option = String.format("-%s/--%s", shortOpt, longOpt);
		} else if (longOpt != null) {
			option = String.format("--%s", longOpt);
		} else {
			option = String.format("-%s", shortOpt);
		}

		return String.format("Too many values given for the option %s - %s positional arguments are allowed", option,
			msg);
	}
}