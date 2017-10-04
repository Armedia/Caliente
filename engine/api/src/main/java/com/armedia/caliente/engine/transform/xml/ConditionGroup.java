
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
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.conditions.CheckExpression;
import com.armedia.caliente.engine.transform.xml.conditions.CustomCheck;
import com.armedia.caliente.engine.transform.xml.conditions.CustomScript;
import com.armedia.caliente.engine.transform.xml.conditions.GroupAnd;
import com.armedia.caliente.engine.transform.xml.conditions.GroupNand;
import com.armedia.caliente.engine.transform.xml.conditions.GroupNor;
import com.armedia.caliente.engine.transform.xml.conditions.GroupNot;
import com.armedia.caliente.engine.transform.xml.conditions.GroupOneof;
import com.armedia.caliente.engine.transform.xml.conditions.GroupOr;
import com.armedia.caliente.engine.transform.xml.conditions.GroupXnor;
import com.armedia.caliente.engine.transform.xml.conditions.GroupXor;
import com.armedia.caliente.engine.transform.xml.conditions.HasAttribute;
import com.armedia.caliente.engine.transform.xml.conditions.HasCalienteProperty;
import com.armedia.caliente.engine.transform.xml.conditions.HasDecorator;
import com.armedia.caliente.engine.transform.xml.conditions.HasValueMapping;
import com.armedia.caliente.engine.transform.xml.conditions.IsAttributeEmpty;
import com.armedia.caliente.engine.transform.xml.conditions.IsAttributeRepeating;
import com.armedia.caliente.engine.transform.xml.conditions.IsAttributeValue;
import com.armedia.caliente.engine.transform.xml.conditions.IsCalientePropertyEmpty;
import com.armedia.caliente.engine.transform.xml.conditions.IsCalientePropertyRepeating;
import com.armedia.caliente.engine.transform.xml.conditions.IsCalientePropertyValue;
import com.armedia.caliente.engine.transform.xml.conditions.IsFirstVersion;
import com.armedia.caliente.engine.transform.xml.conditions.IsLatestVersion;
import com.armedia.caliente.engine.transform.xml.conditions.IsName;
import com.armedia.caliente.engine.transform.xml.conditions.IsOriginalName;
import com.armedia.caliente.engine.transform.xml.conditions.IsOriginalSubtype;
import com.armedia.caliente.engine.transform.xml.conditions.IsReference;
import com.armedia.caliente.engine.transform.xml.conditions.IsSubtype;
import com.armedia.caliente.engine.transform.xml.conditions.IsType;
import com.armedia.caliente.engine.transform.xml.conditions.IsVariableSet;
import com.armedia.caliente.engine.transform.xml.conditions.IsVariableValue;

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
		@XmlElement(name = "has-decorator", type = HasDecorator.class), //

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
	public final boolean check(TransformationContext ctx) throws TransformationException {
		// If there are no elements, then we simply return true
		List<Condition> elements = sanitizeElements(getElements());
		if ((elements == null) || elements.isEmpty()) { return true; }
		return check(elements, ctx);
	}

	protected abstract boolean check(List<Condition> elements, TransformationContext ctx)
		throws TransformationException;
}