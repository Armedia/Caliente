
package com.armedia.caliente.engine.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.xml.conditions.CheckExpression;
import com.armedia.caliente.engine.xml.conditions.CustomCheck;
import com.armedia.caliente.engine.xml.conditions.CustomScript;
import com.armedia.caliente.engine.xml.conditions.GroupAnd;
import com.armedia.caliente.engine.xml.conditions.GroupNand;
import com.armedia.caliente.engine.xml.conditions.GroupNor;
import com.armedia.caliente.engine.xml.conditions.GroupNot;
import com.armedia.caliente.engine.xml.conditions.GroupOneof;
import com.armedia.caliente.engine.xml.conditions.GroupOr;
import com.armedia.caliente.engine.xml.conditions.GroupXnor;
import com.armedia.caliente.engine.xml.conditions.GroupXor;
import com.armedia.caliente.engine.xml.conditions.HasAttribute;
import com.armedia.caliente.engine.xml.conditions.HasCalienteProperty;
import com.armedia.caliente.engine.xml.conditions.HasOriginalSecondarySubtype;
import com.armedia.caliente.engine.xml.conditions.HasSecondarySubtype;
import com.armedia.caliente.engine.xml.conditions.HasValueMapping;
import com.armedia.caliente.engine.xml.conditions.IsAttributeEmpty;
import com.armedia.caliente.engine.xml.conditions.IsAttributeRepeating;
import com.armedia.caliente.engine.xml.conditions.IsAttributeValue;
import com.armedia.caliente.engine.xml.conditions.IsCalientePropertyEmpty;
import com.armedia.caliente.engine.xml.conditions.IsCalientePropertyRepeating;
import com.armedia.caliente.engine.xml.conditions.IsCalientePropertyValue;
import com.armedia.caliente.engine.xml.conditions.IsFirstVersion;
import com.armedia.caliente.engine.xml.conditions.IsLatestVersion;
import com.armedia.caliente.engine.xml.conditions.IsName;
import com.armedia.caliente.engine.xml.conditions.IsOriginalName;
import com.armedia.caliente.engine.xml.conditions.IsOriginalSubtype;
import com.armedia.caliente.engine.xml.conditions.IsReference;
import com.armedia.caliente.engine.xml.conditions.IsSubtype;
import com.armedia.caliente.engine.xml.conditions.IsType;
import com.armedia.caliente.engine.xml.conditions.IsVariableSet;
import com.armedia.caliente.engine.xml.conditions.IsVariableValue;

@XmlTransient
public class ConditionWrapper {

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

	public Condition getCondition() {
		return this.condition;
	}

	public ConditionWrapper setCondition(Condition child) {
		this.condition = child;
		return this;
	}
}