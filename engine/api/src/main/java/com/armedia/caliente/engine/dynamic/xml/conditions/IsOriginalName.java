
package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.store.CmfValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsOriginalName.t")
public class IsOriginalName extends AbstractSingleValueComparison {

	@Override
	protected CmfValue.Type getCandidateType(DynamicElementContext ctx) {
		return CmfValue.Type.STRING;
	}

	@Override
	protected Object getCandidateValue(DynamicElementContext ctx) {
		return ctx.getDynamicObject().getOriginalName();
	}

}