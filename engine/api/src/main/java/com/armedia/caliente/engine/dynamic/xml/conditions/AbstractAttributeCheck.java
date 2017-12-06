
package com.armedia.caliente.engine.dynamic.xml.conditions;

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;

@XmlTransient
public abstract class AbstractAttributeCheck extends AbstractAttributeCalientePropertyVariableCheck {

	@Override
	protected Map<String, DynamicValue> getCandidateValues(DynamicElementContext ctx) {
		return ctx.getDynamicObject().getAtt();
	}

}