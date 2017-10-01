
package com.armedia.caliente.engine.transform.xml.conditions;

import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TypedValue;
import com.armedia.caliente.engine.transform.TransformationContext;

@XmlTransient
public abstract class AbstractVariableCheck extends AbstractAttributeCalientePropertyVariableCheck {

	@Override
	protected final Set<String> getCandidateNames(TransformationContext ctx) {
		return ctx.getVariables().keySet();
	}

	@Override
	protected final TypedValue getCandidate(TransformationContext ctx, String name) {
		return ctx.getVariables().get(name);
	}

}