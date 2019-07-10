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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.xml.CmfObjectArchetypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionClearValueMapping.t", propOrder = {
	"type", "name", "from", "to"
})
public class ValueMappingClear extends ConditionalAction {

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(CmfObjectArchetypeAdapter.class)
	protected CmfObject.Archetype type;

	@XmlElement(name = "name", required = true)
	protected Expression name;

	@XmlElement(name = "from", required = false)
	protected Expression from;

	@XmlElement(name = "to", required = false)
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
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		final CmfObject.Archetype type = Tools.coalesce(getType(), ctx.getDynamicObject().getType());
		String name = Tools.toString(ActionTools.eval(getName(), ctx));
		if (name == null) { throw new ActionException("Must provide a mapping name"); }

		String from = Tools.toString(ActionTools.eval(getFrom(), ctx));
		String to = Tools.toString(ActionTools.eval(getTo(), ctx));
		if ((from == null) && (to == null)) {
			throw new ActionException("Must provide either a sorce or target value to identify the mapping to remove");
		}
		if ((from != null) && (to != null)) {
			throw new ActionException(
				"Must provide only one of either a sorce or target value to identify the mapping to remove (both provided)");
		}

		if (from != null) {
			ctx.getAttributeMapper().clearSourceMapping(type, name, from);
		} else {
			ctx.getAttributeMapper().clearTargetMapping(type, name, to);
		}
	}
}