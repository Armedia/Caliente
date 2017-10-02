
package com.armedia.caliente.engine.transform.xml.conditions;

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TypedValue;

@XmlTransient
public abstract class AbstractVariableCheck extends AbstractAttributeCalientePropertyVariableCheck {

	@Override
	protected Map<String, TypedValue> getCandidateValues(TransformationContext ctx) {
		return ctx.getVariables();
	}

}