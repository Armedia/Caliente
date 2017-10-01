
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ObjectDataMember;
import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsCalientePropertyValue.t", propOrder = {
	"name", "value"
})
public class IsCalientePropertyValue extends AbstractAttributeCalientePropertyVariableValueCheck {

	@Override
	protected ObjectDataMember getCandidate(TransformationContext ctx, String name) {
		return ctx.getObject().getPriv().get(name);
	}

	@Override
	protected Object getCandidateValue(ObjectDataMember candidate, int pos) {
		return candidate.getValues().get(pos);
	}

}