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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.engine.dynamic.xml.ComparisonAdapter;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionRemoveOriginalSecondarySubtypes.t", propOrder = {
	"except"
})
public class OriginalSecondarySubtypeRemove extends ConditionalAction {

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionRemoveOriginalSecondarySubtypes.except.t")
	public static class Except extends Expression {

		@XmlAttribute(name = "comparison")
		@XmlJavaTypeAdapter(ComparisonAdapter.class)
		protected Comparison comparison;

		public Comparison getComparison() {
			return Tools.coalesce(this.comparison, Comparison.DEFAULT);
		}

		public void setComparison(Comparison value) {
			this.comparison = value;
		}

		public boolean check(String candidate, DynamicElementContext<?> ctx) throws ActionException {
			return getComparison().check(CmfValue.Type.STRING, candidate, ActionTools.eval(this, ctx));
		}
	}

	@XmlElement(name = "except", required = false)
	public List<Except> except;

	public List<Except> getExcept() {
		if (this.except == null) {
			this.except = new ArrayList<>();
		}
		return this.except;
	}

	@Override
	protected void executeAction(DynamicElementContext<?> ctx) throws ActionException {
		Set<String> originals = ctx.getDynamicObject().getOriginalSecondarySubtypes();
		if (!originals.isEmpty()) {
			List<Except> except = getExcept();
			if (!except.isEmpty()) {
				Set<String> exceptions = new HashSet<>();
				// Outer iteration is the one with dynamic bounds ...
				for (String s : originals) {
					// Inner iteration will remain the same size for all elements
					for (Except e : getExcept()) {
						if (e.check(s, ctx)) {
							// Matched ... so remove
							exceptions.add(s);
						}
					}
				}

				// If there are changes to be done, execute them
				if (!exceptions.isEmpty()) {
					originals = new LinkedHashSet<>(originals);
					originals.removeAll(exceptions);
				}
			}
		}
		ctx.getDynamicObject().getSecondarySubtypes().removeAll(originals);
	}

}