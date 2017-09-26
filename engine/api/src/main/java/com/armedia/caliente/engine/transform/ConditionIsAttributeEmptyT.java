
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsAttributeEmpty.t")
public class ConditionIsAttributeEmptyT extends ConditionExpressionComparisonT {

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		// TODO implement this condition
		return false;
	}

}