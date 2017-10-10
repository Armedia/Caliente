
package com.armedia.caliente.engine.dynamic.jaxb.conditions;

import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.ObjectContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionGroupOneof.t")
public class GroupOneof extends ConditionGroup {

	@Override
	protected boolean check(List<Condition> elements, ObjectContext ctx) throws ConditionException {
		int trueCount = 0;
		for (Condition c : elements) {
			Objects.requireNonNull(c, "Null conditional elements are not allowed");
			if (c.check(ctx)) {
				trueCount++;
				if (trueCount > 1) { return false; }
			}
		}
		return (trueCount == 1);
	}

}