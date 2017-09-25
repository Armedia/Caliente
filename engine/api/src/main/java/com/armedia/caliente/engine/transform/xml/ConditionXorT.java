
package com.armedia.caliente.engine.transform.xml;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionXor.t")
public class ConditionXorT extends ConditionGroupT {

	@Override
	public <V> boolean evaluate(TransformationContext<V> ctx) {
		int count = 0;
		for (Condition c : getMembers()) {
			Objects.requireNonNull(c, "May not include null conditions in the group");
			if (c.evaluate(ctx)) {
				count++;
			}
		}
		return ((count % 2) == 1);
	}

}