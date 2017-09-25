
package com.armedia.caliente.engine.transform.xml;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionNot.t", propOrder = {
	"element"
})
public class ConditionNotT implements Condition {

	@XmlElements({
		@XmlElement(name = "and", type = ConditionAndT.class), //
		@XmlElement(name = "or", type = ConditionOrT.class), //
		@XmlElement(name = "xor", type = ConditionXorT.class), //
		@XmlElement(name = "not", type = ConditionNotT.class), //
		@XmlElement(name = "nand", type = ConditionNandT.class), //
		@XmlElement(name = "nor", type = ConditionNorT.class), //
		@XmlElement(name = "xnor", type = ConditionXnorT.class), //
		@XmlElement(name = "type", type = ConditionTypeT.class), //
		@XmlElement(name = "subtype", type = ConditionSubtypeT.class), //
		@XmlElement(name = "is-reference", type = ConditionIsReferenceT.class), //
		@XmlElement(name = "check-decorator", type = ConditionDecoratorCheckT.class), //
		@XmlElement(name = "check-object-attribute", type = ConditionObjectAttributeT.class), //
		@XmlElement(name = "check-caliente-property", type = ConditionExternalPropertyT.class), //
		@XmlElement(name = "check-expression", type = ConditionExpressionT.class), //
	})
	protected Condition element;

	public Condition getElement() {
		return this.element;
	}

	public void setElement(Condition element) {
		Objects.requireNonNull(element, "Must provide a non-null element");
		this.element = element;
	}

	@Override
	public boolean evaluate(TransformationContext ctx) {
		Objects.requireNonNull(this.element, "Must set a non-null element");
		return !this.element.evaluate(ctx);
	}
}