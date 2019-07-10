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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.conditions.CheckExpression;
import com.armedia.caliente.engine.dynamic.xml.conditions.CustomCheck;
import com.armedia.caliente.engine.dynamic.xml.conditions.CustomScript;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupAnd;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupNand;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupNor;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupNot;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupOneof;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupOr;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupXnor;
import com.armedia.caliente.engine.dynamic.xml.conditions.GroupXor;
import com.armedia.caliente.engine.dynamic.xml.conditions.HasAttribute;
import com.armedia.caliente.engine.dynamic.xml.conditions.HasCalienteProperty;
import com.armedia.caliente.engine.dynamic.xml.conditions.HasOriginalSecondarySubtype;
import com.armedia.caliente.engine.dynamic.xml.conditions.HasSecondarySubtype;
import com.armedia.caliente.engine.dynamic.xml.conditions.HasValueMapping;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsAttributeEmpty;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsAttributeRepeating;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsAttributeValue;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsCalientePropertyEmpty;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsCalientePropertyRepeating;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsCalientePropertyValue;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsFirstVersion;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsLatestVersion;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsName;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsOriginalName;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsOriginalSubtype;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsReference;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsSubtype;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsType;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsVariableSet;
import com.armedia.caliente.engine.dynamic.xml.conditions.IsVariableValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "condition.t", propOrder = {
	"condition"
})
public class ConditionWrapper implements Condition {

	@XmlElements({
		// First, the groups
		@XmlElement(name = "and", type = GroupAnd.class), //
		@XmlElement(name = "or", type = GroupOr.class), //
		@XmlElement(name = "not", type = GroupNot.class), //
		@XmlElement(name = "xor", type = GroupXor.class), //
		@XmlElement(name = "nand", type = GroupNand.class), //
		@XmlElement(name = "nor", type = GroupNor.class), //
		@XmlElement(name = "xnor", type = GroupXnor.class), //
		@XmlElement(name = "oneof", type = GroupOneof.class), //

		// Now, the non-grouping conditions
		@XmlElement(name = "is-name", type = IsName.class), //
		@XmlElement(name = "is-original-name", type = IsOriginalName.class), //
		@XmlElement(name = "is-type", type = IsType.class), //
		@XmlElement(name = "is-subtype", type = IsSubtype.class), //
		@XmlElement(name = "is-original-subtype", type = IsOriginalSubtype.class), //

		@XmlElement(name = "has-secondary-subtype", type = HasSecondarySubtype.class), //
		@XmlElement(name = "has-original-secondary-subtype", type = HasOriginalSecondarySubtype.class), //

		@XmlElement(name = "is-reference", type = IsReference.class), //
		@XmlElement(name = "is-first-version", type = IsFirstVersion.class), //
		@XmlElement(name = "is-latest-version", type = IsLatestVersion.class), //

		@XmlElement(name = "is-variable-set", type = IsVariableSet.class), //
		@XmlElement(name = "is-variable-value", type = IsVariableValue.class), //

		@XmlElement(name = "has-attribute", type = HasAttribute.class), //
		@XmlElement(name = "is-attribute-value", type = IsAttributeValue.class), //
		@XmlElement(name = "is-attribute-repeating", type = IsAttributeRepeating.class), //
		@XmlElement(name = "is-attribute-empty", type = IsAttributeEmpty.class), //

		@XmlElement(name = "has-caliente-property", type = HasCalienteProperty.class), //
		@XmlElement(name = "is-caliente-property-value", type = IsCalientePropertyValue.class), //
		@XmlElement(name = "is-caliente-property-repeating", type = IsCalientePropertyRepeating.class), //
		@XmlElement(name = "is-caliente-property-empty", type = IsCalientePropertyEmpty.class), //

		@XmlElement(name = "has-value-mapping", type = HasValueMapping.class), //

		@XmlElement(name = "check-expression", type = CheckExpression.class), //
		@XmlElement(name = "custom-script", type = CustomScript.class), //
		@XmlElement(name = "custom-check", type = CustomCheck.class), //

	})
	protected Condition condition;

	protected ConditionWrapper() {
		this(null);
	}

	public ConditionWrapper(Condition condition) {
		this.condition = condition;
	}

	public final boolean hasCondition() {
		return (this.condition != null);
	}

	public final Condition getCondition() {
		return this.condition;
	}

	public ConditionWrapper setCondition(Condition child) {
		this.condition = child;
		return this;
	}

	@Override
	public boolean check(DynamicElementContext ctx) throws ConditionException {
		return hasCondition() && getCondition().check(ctx);
	}
}