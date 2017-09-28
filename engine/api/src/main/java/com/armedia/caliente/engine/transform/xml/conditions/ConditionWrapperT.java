
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.xml.Condition;

@XmlTransient
public class ConditionWrapperT {

	@XmlElements({
		// First, the groups
		@XmlElement(name = "and", type = ConditionGroupAndT.class),
		@XmlElement(name = "or", type = ConditionGroupOrT.class),
		@XmlElement(name = "not", type = ConditionGroupNotT.class),
		@XmlElement(name = "xor", type = ConditionGroupXorT.class),
		@XmlElement(name = "nand", type = ConditionGroupNandT.class),
		@XmlElement(name = "nor", type = ConditionGroupNorT.class),
		@XmlElement(name = "xnor", type = ConditionGroupXnorT.class),
		@XmlElement(name = "oneof", type = ConditionGroupOneofT.class),

		// Now, the non-grouping conditions
		@XmlElement(name = "is-type", type = ConditionIsTypeT.class),
		@XmlElement(name = "is-subtype", type = ConditionIsSubtypeT.class),
		@XmlElement(name = "is-original-subtype", type = ConditionIsOriginalSubtypeT.class),
		@XmlElement(name = "has-decorator", type = ConditionHasDecoratorT.class),

		@XmlElement(name = "is-reference", type = ConditionIsReferenceT.class),
		@XmlElement(name = "is-first-version", type = ConditionIsFirstVersionT.class),
		@XmlElement(name = "is-latest-version", type = ConditionIsLatestVersionT.class),

		@XmlElement(name = "is-variable-set", type = ConditionIsVariableSetT.class),
		@XmlElement(name = "is-variable-value", type = ConditionIsVariableValueT.class),

		@XmlElement(name = "has-attribute", type = ConditionHasAttributeT.class),
		@XmlElement(name = "is-attribute-value", type = ConditionIsAttributeValueT.class),
		@XmlElement(name = "is-attribute-repeating", type = ConditionIsAttributeRepeatingT.class),
		@XmlElement(name = "is-attribute-empty", type = ConditionIsAttributeEmptyT.class),

		@XmlElement(name = "has-caliente-property", type = ConditionHasCalientePropertyT.class),
		@XmlElement(name = "is-caliente-property-value", type = ConditionIsCalientePropertyValueT.class),
		@XmlElement(name = "is-caliente-property-repeating", type = ConditionIsCalientePropertyRepeatingT.class),
		@XmlElement(name = "is-caliente-property-empty", type = ConditionIsCalientePropertyEmptyT.class),

		@XmlElement(name = "has-value-mapping", type = ConditionHasValueMappingT.class),

		@XmlElement(name = "check-expression", type = ConditionCheckExpressionT.class),
		@XmlElement(name = "custom-script", type = ConditionCustomScriptT.class),
		@XmlElement(name = "custom-check", type = ConditionCustomCheckT.class),

	})
	protected Condition condition;

	public Condition getCondition() {
		return this.condition;
	}

	public void setCondition(Condition child) {
		this.condition = child;
	}
}