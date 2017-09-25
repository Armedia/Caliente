
package com.armedia.caliente.engine.transform.xml;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionNand.t")
public class ConditionNandT extends ConditionGroupT {

	@Override
	public <V> boolean evaluate(TransformationContext<V> ctx) {
		for (Condition c : getMembers()) {
			Objects.requireNonNull(c, "May not include null conditions in the group");
			if (!c.evaluate(ctx)) { return true; }
		}
		return false;
	}

}