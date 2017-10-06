
package com.armedia.caliente.engine.xml.conditions;

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TypedValue;

@XmlTransient
public abstract class AbstractCalientePropertyCheck extends AbstractAttributeCalientePropertyVariableCheck {

	@Override
	protected Map<String, TypedValue> getCandidateValues(TransformationContext ctx) {
		return ctx.getObject().getPriv();
	}

}