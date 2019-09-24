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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValueMapper;
import com.armedia.caliente.store.xml.CmfObjectArchetypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasValueMapping.t", propOrder = {
	"type", "name", "from", "to"
})
public class HasValueMapping implements Condition {

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(CmfObjectArchetypeAdapter.class)
	protected CmfObject.Archetype type;

	@XmlElement(name = "name", required = true)
	protected Expression name;

	@XmlElement(name = "name", required = false)
	protected Expression from;

	@XmlElement(name = "name", required = false)
	protected Expression to;

	public void setType(CmfObject.Archetype type) {
		this.type = type;
	}

	public CmfObject.Archetype getType() {
		return this.type;
	}

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression name) {
		this.name = name;
	}

	public Expression getFrom() {
		return this.from;
	}

	public void setFrom(Expression from) {
		this.from = from;
	}

	public Expression getTo() {
		return this.to;
	}

	public void setTo(Expression to) {
		this.to = to;
	}

	@Override
	public boolean check(DynamicElementContext<?> ctx) throws ConditionException {
		CmfObject.Archetype type = getType();
		if (type == null) { throw new ConditionException("No type given to find the mappings with"); }

		Object name = ConditionTools.eval(getName(), ctx);
		if (name == null) { throw new ConditionException("No name given to check for"); }

		CmfValueMapper mapper = ctx.getAttributeMapper();
		Expression key = null;

		key = getFrom();
		if (key != null) {
			Object sourceValue = ConditionTools.eval(key, ctx);
			if (sourceValue == null) { throw new ConditionException("No source value given to search with"); }
			return (mapper.getTargetMapping(getType(), name.toString(), sourceValue.toString()) != null);
		}

		key = getTo();
		if (key != null) {
			Object targetValue = ConditionTools.eval(key, ctx);
			if (targetValue == null) { throw new ConditionException("No target value given to search with"); }
			return (mapper.getSourceMapping(getType(), name.toString(), targetValue.toString()) != null);
		}

		throw new ConditionException("No source or target value given to search with");
	}
}