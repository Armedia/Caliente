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

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractAttributeCalientePropertyVariableCheck extends AbstractExpressionComparison {

	private static final DynamicValue NULL = null;

	protected abstract Map<String, DynamicValue> getCandidateValues(DynamicElementContext ctx);

	protected abstract boolean check(DynamicValue candidate);

	@Override
	public final boolean check(DynamicElementContext ctx) throws ConditionException {
		final String comparand = Tools.toString(ConditionTools.eval(this, ctx));
		final Comparison comparison = getComparison();
		final Map<String, DynamicValue> values = getCandidateValues(ctx);

		if (comparison == Comparison.EQ) {
			// Shortcut!! Look for only one candidate!
			return check(values.get(comparand));
		}

		// Need to find a matching candidate...
		for (String s : values.keySet()) {
			if (comparison.check(CmfValue.Type.STRING, s, comparand)) {
				// Only check the first match, since a true or false result may be conclusive
				return check(values.get(s));
			}
		}

		// None of the matching candidates fulfilled the check...so the result is whatever the NULL
		// value compares to
		return check(AbstractAttributeCalientePropertyVariableCheck.NULL);
	}

}