
package com.armedia.caliente.engine.transform.xml.conditions;

import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.ObjectDataMember;
import com.armedia.caliente.engine.transform.TransformationContext;

@XmlTransient
public abstract class AbstractCalientePropertyCheck extends AbstractAttributeCalientePropertyVariableCheck {

	@Override
	protected final Set<String> getCandidateNames(TransformationContext ctx) {
		return ctx.getObject().getPriv().keySet();
	}

	@Override
	protected final ObjectDataMember getCandidate(TransformationContext ctx, String name) {
		return ctx.getObject().getPriv().get(name);
	}

}