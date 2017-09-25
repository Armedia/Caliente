
package com.armedia.caliente.engine.transform.xml;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "condition.t", propOrder = {
	"condition"
})
public class ConditionT implements Condition {

	@XmlElements({
		@XmlElement(name = "and", type = ConditionAndT.class), //
		@XmlElement(name = "or", type = ConditionOrT.class), //
		@XmlElement(name = "xor", type = ConditionXorT.class), //
		@XmlElement(name = "not", type = ConditionNotT.class), //
		@XmlElement(name = "nand", type = ConditionNandT.class), //
		@XmlElement(name = "nor", type = ConditionNorT.class), //
		@XmlElement(name = "xnor", type = ConditionXnorT.class), //
	})
	protected Condition condition;

	public Condition getMember() {
		return this.condition;
	}

	public void setMember(Condition condition) {
		this.condition = condition;
	}

	@Override
	public <V> boolean evaluate(TransformationContext<V> ctx) {
		Objects.requireNonNull(this.condition, "Must set a non-null condition");
		return this.condition.evaluate(ctx);
	}

}