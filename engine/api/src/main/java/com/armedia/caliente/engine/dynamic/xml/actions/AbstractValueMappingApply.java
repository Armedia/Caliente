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

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.Cardinality;
import com.armedia.caliente.engine.dynamic.xml.CardinalityAdapter;
import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.engine.dynamic.xml.ComparisonAdapter;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper.Mapping;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractValueMappingApply<E extends Enum<E>> extends ConditionalAction {

	@XmlElement(name = "comparison", required = false)
	@XmlJavaTypeAdapter(ComparisonAdapter.class)
	protected Comparison comparison;

	@XmlElement(name = "attribute-name", required = true)
	protected Expression attributeName;

	@XmlElement(name = "cardinality", required = false)
	@XmlJavaTypeAdapter(CardinalityAdapter.class)
	protected Cardinality cardinality;

	@XmlElement(name = "fallback", required = false)
	protected Expression fallback;

	public Comparison getComparison() {
		return Tools.coalesce(this.comparison, Comparison.DEFAULT);
	}

	public void setComparison(Comparison comparison) {
		this.comparison = comparison;
	}

	public Expression getAttributeName() {
		return this.attributeName;
	}

	public void setAttributeName(Expression attributeName) {
		this.attributeName = attributeName;
	}

	public Expression getFallback() {
		return this.fallback;
	}

	public void setFallback(Expression fallback) {
		this.fallback = fallback;
	}

	public abstract void setType(E type);

	public abstract E getType();

	protected abstract String getMappedLabel(DynamicElementContext<?> ctx) throws ActionException;

	protected abstract CmfObject.Archetype getMappingType(E type);

	public Cardinality getCardinality() {
		return Tools.coalesce(this.cardinality, Cardinality.ALL);
	}

	public void setCardinality(Cardinality cardinality) {
		this.cardinality = cardinality;
	}

	private String mapValue(DynamicElementContext<?> ctx, CmfObject.Archetype mappingType, String mappingName,
		String sourceValue, CmfValue.Type targetType) throws ActionException {
		Mapping m = ctx.getAttributeMapper().getTargetMapping(mappingType, mappingName, sourceValue);
		return (m != null ? m.getTargetValue() : null);
	}

	private void applyMapping(DynamicElementContext<?> ctx, CmfObject.Archetype type, String mappingName,
		DynamicValue candidate) throws ActionException {

		if (!candidate.isMultivalued()) {
			// Cardinality is irrelevant...
			String oldString = Tools.toString(candidate.getValue());
			String newString = mapValue(ctx, type, mappingName, oldString, candidate.getType());
			if (newString == null) {
				// Try a fallback value
				newString = Tools.toString(ActionTools.eval(getFallback(), ctx));
			}
			if ((newString != null) && !StringUtils.equals(oldString, newString)) {
				candidate.setValue(newString);
			}
			return;
		}

		final int valueCount = candidate.getSize();
		if (valueCount > 0) {
			final List<Object> newValues = new LinkedList<>();
			final Cardinality cardinality = getCardinality();
			switch (cardinality) {
				case ALL:
					for (Object oldValue : candidate.getValues()) {
						String oldString = Tools.toString(oldValue);
						String newValue = mapValue(ctx, type, mappingName, oldString, candidate.getType());
						newValues.add(Tools.coalesce(newValue, oldValue));
					}
					break;

				case FIRST:
				case LAST:
					for (Object oldValue : candidate.getValues()) {
						newValues.add(oldValue);
					}
					int targetIndex = (cardinality == Cardinality.FIRST ? 0 : valueCount - 1);
					String oldString = Tools.toString(newValues.remove(targetIndex));
					String newValue = mapValue(ctx, type, mappingName, oldString, candidate.getType());
					newValues.add(targetIndex, Tools.coalesce(newValue, oldString));
					break;
			}
			candidate.setValues(newValues);
		}
	}

	@Override
	protected void executeAction(DynamicElementContext<?> ctx) throws ActionException {
		final CmfObject.Archetype type = Tools.coalesce(getMappingType(getType()), ctx.getDynamicObject().getType());
		final String comparand = Tools.toString(ActionTools.eval(getAttributeName(), ctx));
		if (comparand == null) { throw new ActionException("No comparand given to check the name against"); }
		final Comparison comparison = getComparison();
		final String mappingName = getMappedLabel(ctx);
		if (mappingName == null) { throw new ActionException("No mapping name given to apply"); }

		if (comparison == Comparison.EQ) {
			// Shortcut!! Look for only one candidate!
			DynamicValue candidate = ctx.getDynamicObject().getAtt().get(comparand);
			if (candidate != null) {
				applyMapping(ctx, type, mappingName, candidate);
			}
			return;
		}

		// Need to find a matching candidate...
		for (String s : ctx.getDynamicObject().getAtt().keySet()) {
			if (comparison.check(CmfValue.Type.STRING, s, comparand)) {
				applyMapping(ctx, type, mappingName, ctx.getDynamicObject().getAtt().get(s));
			}
		}
	}
}