
package com.armedia.caliente.engine.dynamic.jaxb.conditions;

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;

@XmlTransient
public abstract class AbstractCalientePropertyCheck extends AbstractAttributeCalientePropertyVariableCheck {

	@Override
	protected Map<String, DynamicValue> getCandidateValues(DynamicElementContext ctx) {
		return ctx.getTransformableObject().getPriv();
	}

}