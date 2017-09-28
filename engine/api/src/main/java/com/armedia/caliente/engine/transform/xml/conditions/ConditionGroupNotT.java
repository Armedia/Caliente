
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.Condition;
import com.armedia.caliente.engine.transform.xml.ConditionWrapper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionGroupNot.t", propOrder = {
	"condition"
})
public class ConditionGroupNotT extends ConditionWrapper implements Condition {

	@Override
	public boolean check(TransformationContext ctx) {
		final Condition condition = getCondition();
		if (condition == null) { return true; }
		return !condition.check(ctx);
	}

}