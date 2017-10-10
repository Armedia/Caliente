
package com.armedia.caliente.engine.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ConditionException;
import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.engine.xml.Condition;
import com.armedia.caliente.engine.xml.ConditionWrapper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionGroupNot.t")
public class GroupNot extends ConditionWrapper {

	@Override
	public boolean check(ObjectContext ctx) throws ConditionException {
		final Condition condition = getCondition();
		if (condition == null) { return true; }
		return !condition.check(ctx);
	}

}