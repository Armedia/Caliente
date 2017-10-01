
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ObjectDataMember;
import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasAttributeValue.t", propOrder = {
	"name", "value"
})
public class IsAttributeValue extends AbstractAttributeCalientePropertyVariableValueCheck {

	@Override
	protected ObjectDataMember getCandidate(TransformationContext ctx, String name) {
		return ctx.getObject().getAtt().get(name);
	}

	@Override
	protected Object getCandidateValue(ObjectDataMember candidate, int pos) {
		return candidate.getValues().get(pos);
	}

}