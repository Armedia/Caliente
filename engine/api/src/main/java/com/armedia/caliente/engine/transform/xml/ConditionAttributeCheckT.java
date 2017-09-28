
package com.armedia.caliente.engine.transform.xml;

import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfValue;

@XmlTransient
public abstract class ConditionAttributeCheckT extends ConditionAttributeCalientePropertyVariableCheckT<CmfAttribute<CmfValue>> {

	@Override
	protected final Set<String> getCandidateNames(TransformationContext ctx) {
		return ctx.getPropertyNames();
	}

	@Override
	protected final CmfAttribute<CmfValue> getCandidate(TransformationContext ctx, String name) {
		return ctx.getAttribute(name);
	}

}