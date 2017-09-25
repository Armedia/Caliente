
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionGroup.t", propOrder = {
	"members"
})
public abstract class ConditionGroupT implements Condition {

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
	private List<Condition> members;

	public List<Condition> getMembers() {
		if (this.members == null) {
			this.members = new ArrayList<>();
		}
		return this.members;
	}

}