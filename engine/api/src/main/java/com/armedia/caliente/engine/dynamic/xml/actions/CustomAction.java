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

package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.Action;
import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.ActionFactory;
import com.armedia.caliente.engine.dynamic.CustomComponents;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionCustomAction.t", propOrder = {
	"className"
})
public class CustomAction extends ConditionalAction {

	@XmlElement(name = "class-name", required = true)
	protected Expression className;

	public Expression getClassName() {
		return this.className;
	}

	public void setClassName(Expression value) {
		this.className = value;
	}

	@Override
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		String className = Tools.toString(ActionTools.eval(getClassName(), ctx));
		if (className == null) { throw new ActionException("No classname given to insantiate"); }

		final ActionFactory factory = CustomComponents.getActionFactory(className);
		if (factory == null) {
			throw new ActionException(String.format("Failed to locate an action factory for class [%s]", className));
		}
		try {
			Action action = factory.acquireInstance(className);
			try {
				action.apply(ctx);
			} finally {
				try {
					factory.releaseInstance(action);
				} catch (Exception e) {
					this.log.warn("Failed to release an Action instance of {}", className, e);
				}
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}
}