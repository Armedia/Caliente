
package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.store.CmfValueType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsOriginalSubtype.t")
public class IsOriginalSubtype extends AbstractSingleValueComparison {

	@Override
	protected CmfValueType getCandidateType(DynamicElementContext ctx) {
		return CmfValueType.STRING;
	}

	@Override
	protected Object getCandidateValue(DynamicElementContext ctx) {
		return ctx.getDynamicObject().getOriginalSubtype();
	}

}