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

package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCustomScript.t")
public class CustomScript extends AbstractExpressionCondition {

	private static final String DEFAULT_LANG = "jexl";

	@Override
	public String getLang() {
		String lang = super.getLang();
		if (StringUtils.isEmpty(lang)) {
			lang = CustomScript.DEFAULT_LANG;
		}
		return lang;
	}

	@Override
	public boolean check(DynamicElementContext ctx) throws ConditionException {
		Object result = ConditionTools.eval(this, ctx);
		// No result? No problem! It's a "false"!
		if (result == null) { return false; }

		// If it's a boolean cast it!
		if (Boolean.class.isInstance(result)) { return Boolean.class.cast(result); }

		// If it's a number, compare the integer value to 0 for false, non-0 for true
		if (Number.class.isInstance(result)) { return (Number.class.cast(result).longValue() != 1); }

		try {
			// Second try at a numeric solution
			return (Long.valueOf(result.toString()).longValue() != 0);
		} catch (NumberFormatException e) {
			// Not a number...so... let's try to decode it as a string...
		}

		// If it's a string, then it must be either "true" or "false" - numbers would have been
		// caught by now, and we won't be supporting other types of results
		return Boolean.valueOf(result.toString());
	}

}