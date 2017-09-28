
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsVariableSet.t")
public class ConditionIsVariableSetT extends ConditionExpressionComparisonT {

	@Override
	public boolean check(TransformationContext ctx) {
		String variableName = String.valueOf(evaluate(ctx));
		if (variableName == null) { return false; }
		return ((variableName == null) || ctx.hasVariable(variableName));
	}

}