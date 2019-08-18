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
package com.armedia.caliente.tools.scratchpad;

import java.util.Map;

import org.apache.commons.text.StringSubstitutor;

public class CLIConst {

	public static final String DEFAULT_LOG_FORMAT = "caliente-tools-scratchpad-${logTimeStamp}";

	public static final String getLogName(Map<String, ?> properties) {
		return StringSubstitutor.replace(CLIConst.DEFAULT_LOG_FORMAT, properties);
	}

	public static final String getLogName() {
		return StringSubstitutor.replaceSystemProperties(CLIConst.DEFAULT_LOG_FORMAT);
	}
}