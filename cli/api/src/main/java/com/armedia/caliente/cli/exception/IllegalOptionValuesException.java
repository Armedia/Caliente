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

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValueFilter;
import com.armedia.commons.utilities.Tools;

public class IllegalOptionValuesException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final Set<String> values;
	private final String definition;

	public IllegalOptionValuesException(OptionScheme optionScheme, Option option, Set<String> values) {
		super(optionScheme, option, null);
		this.values = Tools.freezeCopy(values);
		OptionValueFilter filter = option.getValueFilter();
		String definition = null;
		if (filter != null) {
			definition = StringUtils.strip(filter.getDefinition());
		}
		if (!StringUtils.isBlank(definition)) {
			this.definition = String.format(", must be %s", option.getValueFilter().getDefinition());
		} else {
			this.definition = "";
		}
	}

	public Set<String> getValues() {
		return this.values;
	}

	@Override
	protected String renderMessage() {
		Option o = getOption();
		String longOpt = o.getLongOpt();
		Character shortOpt = o.getShortOpt();
		String label = "";
		final String plural = (this.values.size() == 1 ? "" : "s");
		final String is_are = (this.values.size() == 1 ? "is" : "are");
		if ((longOpt != null) && (shortOpt != null)) {
			label = String.format("-%s/--%s", shortOpt, longOpt);
		} else if (longOpt != null) {
			label = String.format("--%s", longOpt);
		} else {
			label = String.format("-%s", shortOpt);
		}
		return String.format("The value%s %s %s not valid for the option %s%s", plural, this.values, is_are, label,
			this.definition);
	}
}