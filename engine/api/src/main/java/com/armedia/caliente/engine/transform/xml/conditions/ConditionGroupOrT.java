
package com.armedia.caliente.engine.transform.xml.conditions;

import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.Condition;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionGroupOr.t")
public class ConditionGroupOrT extends AbstractGroupingCondition {

	@Override
	protected boolean doEvaluate(List<Condition> elements, TransformationContext ctx) {
		for (Condition c : elements) {
			Objects.requireNonNull(c, "Null conditional elements are not allowed");
			if (c.check(ctx)) { return true; }
		}
		return false;
	}
}