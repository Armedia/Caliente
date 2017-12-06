
package com.armedia.caliente.engine.dynamic.xml.conditions;

import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionGroupNor.t")
public class GroupNor extends ConditionGroup {

	@Override
	protected boolean check(List<Condition> elements, DynamicElementContext ctx) throws ConditionException {
		for (Condition c : elements) {
			Objects.requireNonNull(c, "Null conditional elements are not allowed");
			if (c.check(ctx)) { return false; }
		}
		return true;
	}

}