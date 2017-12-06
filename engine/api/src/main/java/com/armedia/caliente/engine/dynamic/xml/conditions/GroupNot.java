
package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionWrapper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionGroupNot.t")
public class GroupNot extends ConditionWrapper {

	@Override
	public boolean check(DynamicElementContext ctx) throws ConditionException {
		final Condition condition = getCondition();
		if (condition == null) { return true; }
		return !condition.check(ctx);
	}

}