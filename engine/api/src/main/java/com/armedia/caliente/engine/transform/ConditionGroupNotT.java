
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionGroupNot.t", propOrder = {
	"condition"
})
public class ConditionGroupNotT extends ConditionWrapperT implements Condition {

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		final Condition condition = getCondition();
		if (condition == null) { return true; }
		return !condition.check(ctx);
	}

}