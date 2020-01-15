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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.xml.CmfValueTypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractSetValue extends ConditionalAction {

	@XmlElement(name = "name", required = true)
	protected Expression name;

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(CmfValueTypeAdapter.class)
	protected CmfValue.Type type;

	@XmlElement(name = "value", required = true)
	protected Expression value;

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression value) {
		this.name = value;
	}

	public CmfValue.Type getType() {
		return Tools.coalesce(this.type, CmfValue.Type.STRING);
	}

	public void setDataType(CmfValue.Type value) {
		this.type = value;
	}

	public Expression getValue() {
		return this.value;
	}

	public void setValue(Expression value) {
		this.value = value;
	}

	protected abstract DynamicValue createValue(DynamicElementContext<?> ctx, String name, CmfValue.Type type,
		boolean multivalue);

	private Iterable<?> toIterable(Object o) {
		if (o == null) { return null; }
		if (Iterable.class.isInstance(o)) { return Iterable.class.cast(o); }
		if (o.getClass().isArray()) { return Arrays.asList((Object[]) o); }
		return Collections.singletonList(o);
	}

	private Object fromIterable(Object o) {
		if (o == null) { return null; }
		if (Iterable.class.isInstance(o) || o.getClass().isArray()) {
			Iterable<?> iterable = (Iterable.class.isInstance(o) ? Iterable.class.cast(o)
				: Arrays.asList((Object[]) o));
			Iterator<?> iterator = iterable.iterator();
			if (iterator.hasNext()) {
				o = iterator.next();
			}
		}
		return o;
	}

	@Override
	protected final void executeAction(DynamicElementContext<?> ctx) throws ActionException {
		String name = Tools.toString(ActionTools.eval(getName(), ctx));
		if (name == null) { throw new ActionException("No name expression given for variable definition"); }

		final CmfValue.Type type = getType();
		final Object value = ActionTools.eval(getValue(), ctx);
		final boolean repeating = (Iterable.class.isInstance(value) || ((value != null) && value.getClass().isArray()));
		final DynamicValue variable = createValue(ctx, name, type, repeating);
		if (value != null) {
			if (repeating) {
				// Make sure we take all available values
				variable.setValues(toIterable(value));
			} else {
				// Make sure we take only the first value
				variable.setValue(fromIterable(value));
			}
		}
		ctx.getDynamicObject().getAtt().put(name, variable);
	}

}