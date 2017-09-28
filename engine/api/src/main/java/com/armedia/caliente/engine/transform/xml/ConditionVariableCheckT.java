
package com.armedia.caliente.engine.transform.xml;

import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;

@XmlTransient
public abstract class ConditionVariableCheckT
	extends ConditionAttributeCalientePropertyVariableCheckT<CmfProperty<CmfValue>> {

	@Override
	protected final Set<String> getCandidateNames(TransformationContext ctx) {
		return ctx.getVariableNames();
	}

	@Override
	protected final CmfProperty<CmfValue> getCandidate(TransformationContext ctx, String name) {
		return ctx.getVariable(name);
	}

}