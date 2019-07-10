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

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.commons.utilities.Tools;

public class MissingRequiredOptionsException extends CommandLineSyntaxException {
	private static final long serialVersionUID = 1L;

	private final Collection<Option> baseMissing;
	private final String command;
	private final Collection<Option> commandMissing;

	public MissingRequiredOptionsException(OptionScheme scheme, Collection<Option> baseMissing, String command,
		Collection<Option> commandMissing) {
		super(scheme, null, null);
		this.baseMissing = (baseMissing.isEmpty() ? null : Tools.freezeCollection(new ArrayList<>(baseMissing)));
		if ((command != null) && (commandMissing != null) && !commandMissing.isEmpty()) {
			this.command = command;
			this.commandMissing = Tools.freezeCollection(new ArrayList<>(commandMissing));
		} else {
			this.command = null;
			this.commandMissing = null;
		}
	}

	public Collection<Option> getBaseMissing() {
		return this.baseMissing;
	}

	public String getCommand() {
		return this.command;
	}

	public Collection<Option> getCommandMissing() {
		return this.commandMissing;
	}

	@Override
	protected String renderMessage() {
		String globalMsg = "";
		if ((this.baseMissing != null) && !this.baseMissing.isEmpty()) {
			globalMsg = String.format("The following required global options were not specified: %s",
				this.baseMissing.stream().map(Option::getKey).collect(Collectors.toCollection(TreeSet::new)));
		}
		String commandMsg = "";
		if (this.command != null) {
			commandMsg = String.format("%she following options required for the '%s' command were not specified: %s",
				(StringUtils.isEmpty(globalMsg) ? "T" : ", and t"), this.command,
				this.commandMissing.stream().map(Option::getKey).collect(Collectors.toCollection(TreeSet::new)));
		}
		return String.format("%s%s", globalMsg, commandMsg);
	}
}