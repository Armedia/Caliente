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
package com.armedia.caliente.engine.dynamic.xml;

import java.util.regex.Pattern;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class AutoCDATAAdapter extends XmlAdapter<String, String> {

	private static final Pattern CDATA_CHECKER = Pattern.compile("[&<>]");

	@Override
	public String marshal(String value) throws Exception {
		if (AutoCDATAAdapter.CDATA_CHECKER.matcher(value).find()) {
			value = String.format("<![CDATA[%s]]>", value);
		}
		return value;
	}

	@Override
	public String unmarshal(String value) throws Exception {
		return value;
	}

}