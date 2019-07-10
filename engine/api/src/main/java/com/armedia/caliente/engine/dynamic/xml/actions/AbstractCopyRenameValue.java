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

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractCopyRenameValue extends ConditionalAction {

	@XmlElement(name = "from", required = true)
	protected Expression from;

	@XmlElement(name = "to", required = true)
	protected Expression to;

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

	protected abstract Map<String, DynamicValue> getCandidateValues(DynamicElementContext ctx);

	protected abstract void storeValue(DynamicElementContext ctx, DynamicValue oldValue, DynamicValue newValue);

	@Override
	protected final void executeAction(DynamicElementContext ctx) throws ActionException {
		String from = StringUtils.strip(Tools.toString(ActionTools.eval(getFrom(), ctx)));
		if (StringUtils.isEmpty(from)) { throw new ActionException("No name expression given for element to copy"); }
		String to = StringUtils.strip(Tools.toString(ActionTools.eval(getFrom(), ctx)));
		if (StringUtils.isEmpty(to)) { throw new ActionException("No name expression given for element to create"); }

		final Map<String, DynamicValue> values = getCandidateValues(ctx);
		DynamicValue src = values.get(Tools.toString(from));
		if (src != null) {
			DynamicValue tgt = new DynamicValue(Tools.toString(to), src.getType(), src.isMultivalued());
			tgt.setValues(src.getValues());
			storeValue(ctx, src, tgt);
		}
	}

}