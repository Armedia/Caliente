
package com.armedia.caliente.engine.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.store.CmfDataType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsSubtype.t")
public class IsSubtype extends AbstractSingleValueComparison {

	@Override
	protected CmfDataType getCandidateType(ObjectContext ctx) {
		return CmfDataType.STRING;
	}

	@Override
	protected Object getCandidateValue(ObjectContext ctx) {
		return ctx.getTransformableObject().getSubtype();
	}

}