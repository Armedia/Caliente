
package com.armedia.caliente.engine.dynamic.xml.conditions;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsVariableValue.t", propOrder = {
	"name", "value"
})
public class IsVariableValue extends AbstractAttributeCalientePropertyVariableValueCheck {

	@Override
	protected Map<String, DynamicValue> getCandidateValues(DynamicElementContext ctx) {
		return ctx.getVariables();
	}

}