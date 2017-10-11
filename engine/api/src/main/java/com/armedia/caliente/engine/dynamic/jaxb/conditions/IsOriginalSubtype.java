
package com.armedia.caliente.engine.dynamic.jaxb.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.store.CmfDataType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsOriginalSubtype.t")
public class IsOriginalSubtype extends AbstractSingleValueComparison {

	@Override
	protected CmfDataType getCandidateType(DynamicElementContext ctx) {
		return CmfDataType.STRING;
	}

	@Override
	protected Object getCandidateValue(DynamicElementContext ctx) {
		return ctx.getTransformableObject().getOriginalSubtype();
	}

}