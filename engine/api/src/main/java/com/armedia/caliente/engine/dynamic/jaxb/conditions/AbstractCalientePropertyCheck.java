
package com.armedia.caliente.engine.dynamic.jaxb.conditions;

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.dynamic.ObjectContext;
import com.armedia.caliente.engine.dynamic.TypedValue;

@XmlTransient
public abstract class AbstractCalientePropertyCheck extends AbstractAttributeCalientePropertyVariableCheck {

	@Override
	protected Map<String, TypedValue> getCandidateValues(ObjectContext ctx) {
		return ctx.getTransformableObject().getPriv();
	}

}