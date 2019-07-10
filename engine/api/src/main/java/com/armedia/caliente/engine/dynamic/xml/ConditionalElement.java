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

package com.armedia.caliente.engine.dynamic.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.ImmutableElementContext;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

@XmlTransient
public abstract class ConditionalElement {

	@XmlTransient
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElement(name = "if", required = false)
	protected ConditionWrapper condition;

	public Condition getCondition() {
		if (this.condition == null) { return null; }
		return this.condition.getCondition();
	}

	public ConditionalElement setCondition(Condition condition) {
		if (condition == null) {
			this.condition = null;
		} else {
			this.condition = new ConditionWrapper(condition);
		}
		return this;
	}

	protected final boolean checkCondition(DynamicElementContext ctx) throws ConditionException {
		final Condition condition = getCondition();
		if (condition == null) { return true; }
		ImmutableElementContext immutable = null;
		if (ImmutableElementContext.class.isInstance(ctx)) {
			// Small tweak in hopes of optimization...
			immutable = ImmutableElementContext.class.cast(ctx);
		} else {
			immutable = new ImmutableElementContext(ctx);
		}
		return condition.check(immutable);
	}

	protected final String getObjectDescription(DynamicElementContext ctx) {
		CmfObject<CmfValue> obj = ctx.getBaseObject();
		if (obj == null) { return null; }
		return obj.getDescription();
	}
}