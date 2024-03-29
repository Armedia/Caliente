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

package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.engine.dynamic.xml.ComparisonAdapter;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractTransformValue extends ConditionalAction {

	@XmlElement(name = "comparison", required = true)
	@XmlJavaTypeAdapter(ComparisonAdapter.class)
	protected Comparison comparison;

	@XmlElement(name = "name", required = true)
	protected Expression name;

	public Comparison getComparison() {
		return Tools.coalesce(this.comparison, Comparison.DEFAULT);
	}

	public void setComparison(Comparison value) {
		this.comparison = value;
	}

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression value) {
		this.name = value;
	}

	protected abstract Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx);

	protected abstract DynamicValue executeAction(DynamicElementContext<?> ctx, DynamicValue candidate)
		throws ActionException;

	protected boolean failShort() {
		return false;
	}

	@Override
	protected final void executeAction(DynamicElementContext<?> ctx) throws ActionException {
		if (failShort()) { return; }
		final String comparand = Tools.toString(ActionTools.eval(getName(), ctx));
		if (comparand == null) { throw new ActionException("No comparand given to check the name against"); }
		final Comparison comparison = getComparison();

		final Map<String, DynamicValue> values = getCandidateValues(ctx);

		if (comparison == Comparison.EQ) {
			// Shortcut!! Look for only one candidate!
			DynamicValue candidate = values.get(comparand);
			if (candidate != null) {
				DynamicValue result = executeAction(ctx, candidate);
				if ((result != null) && (result != candidate)) {
					values.remove(candidate.getName());
					values.put(result.getName(), result);
				}
			}
			return;
		}

		// Need to find a matching candidate...
		for (String s : values.keySet()) {
			if (comparison.check(CmfValue.Type.STRING, s, comparand)) {
				DynamicValue candidate = values.get(s);
				DynamicValue result = executeAction(ctx, candidate);
				if ((result != null) && (result != candidate)) {
					values.remove(candidate.getName());
					values.put(result.getName(), result);
				}
			}
		}
	}

}