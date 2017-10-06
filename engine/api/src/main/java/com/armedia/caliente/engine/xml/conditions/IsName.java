
package com.armedia.caliente.engine.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfDataType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsName.t")
public class IsName extends AbstractSingleValueComparison {

	@Override
	protected CmfDataType getCandidateType(TransformationContext ctx) {
		return CmfDataType.STRING;
	}

	@Override
	protected Object getCandidateValue(TransformationContext ctx) {
		return ctx.getObject().getName();
	}

}