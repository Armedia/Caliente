
package com.armedia.caliente.engine.transform.xml;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionOr.t")
public class ConditionOrT extends ConditionGroupT {

	@Override
	public boolean evaluate(TransformationContext ctx) {
		for (Condition c : getMembers()) {
			Objects.requireNonNull(c, "May not include null conditions in the group");
			if (c.evaluate(ctx)) { return true; }
		}
		return false;
	}

}