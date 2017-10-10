
package com.armedia.caliente.engine.xml.conditions;

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.engine.transform.TypedValue;

@XmlTransient
public abstract class AbstractVariableCheck extends AbstractAttributeCalientePropertyVariableCheck {

	@Override
	protected Map<String, TypedValue> getCandidateValues(ObjectContext ctx) {
		return ctx.getVariables();
	}

}