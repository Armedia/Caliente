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

package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.store.CmfValue;

@XmlTransient
public abstract class AbstractSingleValueComparison extends AbstractExpressionComparison {

	protected abstract CmfValue.Type getCandidateType(DynamicElementContext ctx);

	protected abstract Object getCandidateValue(DynamicElementContext ctx);

	@Override
	public final boolean check(DynamicElementContext ctx) throws ConditionException {
		Object comparand = ConditionTools.eval(this, ctx);
		if (comparand == null) { throw new ConditionException("No value given to compare against"); }
		return getComparison().check(getCandidateType(ctx), getCandidateValue(ctx), comparand);
	}

}