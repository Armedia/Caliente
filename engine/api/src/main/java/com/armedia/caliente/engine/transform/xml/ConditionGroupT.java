
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionGroup.t", propOrder = {
	"elements"
})
public abstract class ConditionGroupT implements Condition {

	@XmlElements({
		// First, the groups
		@XmlElement(name = "and", type = ConditionGroupAndT.class),
		@XmlElement(name = "or", type = ConditionGroupOrT.class),
		@XmlElement(name = "not", type = ConditionGroupNotT.class),
		@XmlElement(name = "xor", type = ConditionGroupXorT.class),
		@XmlElement(name = "nand", type = ConditionGroupNandT.class),
		@XmlElement(name = "nor", type = ConditionGroupNorT.class),
		@XmlElement(name = "xnor", type = ConditionGroupXnorT.class),
		@XmlElement(name = "mux", type = ConditionGroupMuxT.class),

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
	public final boolean check(TransformationContext ctx) {
		// If there are no elements, then we simply return true
		List<Condition> elements = sanitizeElements(getElements());
		if ((elements == null) || elements.isEmpty()) { return true; }
		return doEvaluate(elements, ctx);
	}

	protected abstract boolean doEvaluate(List<Condition> elements, TransformationContext ctx);
}