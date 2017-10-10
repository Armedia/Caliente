
package com.armedia.caliente.engine.xml.conditions;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.engine.transform.TypedValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsCalientePropertyValue.t", propOrder = {
	"name", "value"
})
public class IsCalientePropertyValue extends AbstractAttributeCalientePropertyVariableValueCheck {

	@Override
	protected Map<String, TypedValue> getCandidateValues(ObjectContext ctx) {
		return ctx.getTransformableObject().getPriv();
	}

}