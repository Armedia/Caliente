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

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionAddSecondarySubtype.t", propOrder = {
	"name"
})
public class SecondarySubtypeAdd extends ConditionalAction {

	@XmlElement(name = "name", required = true)
	protected Expression name;

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression name) {
		this.name = name;
	}

	@Override
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		String secondary = Tools.toString(ActionTools.eval(getName(), ctx));
		if (StringUtils.isEmpty(secondary)) { return; }

		for (String s : Tools.splitEscaped(',', secondary)) {
			s = StringUtils.strip(s);
			if (!StringUtils.isEmpty(s)) {
				ctx.getDynamicObject().getSecondarySubtypes().add(s);
			}
		}
	}

}