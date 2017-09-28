
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasDecorator.t")
public class ConditionHasDecoratorT extends ConditionExpressionComparisonT {

	@Override
	public boolean check(TransformationContext ctx) {
		// TODO implement this condition
		return false;
	}

}