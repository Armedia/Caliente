
package com.armedia.caliente.engine.transform.xml;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionXnor.t")
public class ConditionXnorT extends ConditionGroupT {

	@Override
	public boolean evaluate(TransformationContext ctx) {
		int count = 0;
		for (Condition c : getMembers()) {
			Objects.requireNonNull(c, "May not include null conditions in the group");
			if (c.evaluate(ctx)) {
				count++;
			}
		}
		return ((count % 2) == 0);
	}

}