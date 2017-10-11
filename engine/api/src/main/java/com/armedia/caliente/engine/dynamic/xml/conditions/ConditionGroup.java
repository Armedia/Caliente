
package com.armedia.caliente.engine.dynamic.xml.conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionGroup.t", propOrder = {
	"elements"
})
public abstract class ConditionGroup implements Condition {

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
	protected List<Condition> elements;

	public List<Condition> getElements() {
		if (this.elements == null) {
			this.elements = new ArrayList<>();
		}
		return this.elements;
	}

	private List<Condition> sanitizeElements(List<Condition> elements) {
		if (elements == null) {
			elements = Collections.emptyList();
		}
		List<Condition> ret = new ArrayList<>(elements.size());
		for (Condition c : elements) {
			if (c != null) {
				ret.add(c);
			}
		}
		return ret;
	}

	@Override
	public final boolean check(DynamicElementContext ctx) throws ConditionException {
		// If there are no elements, then we simply return true
		List<Condition> elements = sanitizeElements(getElements());
		if ((elements == null) || elements.isEmpty()) { return true; }
		return check(elements, ctx);
	}

	protected abstract boolean check(List<Condition> elements, DynamicElementContext ctx) throws ConditionException;
}