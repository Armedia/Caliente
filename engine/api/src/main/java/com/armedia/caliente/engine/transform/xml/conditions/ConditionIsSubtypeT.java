
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfDataType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsSubtype.t")
public class ConditionIsSubtypeT extends ConditionExpressionComparisonT {

	@Override
	public boolean check(TransformationContext ctx) {
		return getComparison().check(CmfDataType.STRING, evaluate(ctx), ctx.getSubtype());
	}

}