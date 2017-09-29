
package com.armedia.caliente.engine.transform.xml.actions;

import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfValue;

@XmlTransient
public abstract class AbstractTransformValueAttribute extends AbstractTransformValue {

	@Override
	protected final Set<String> getCandidateNames(TransformationContext ctx) {
		return ctx.getAttributeNames();
	}

	@Override
	protected final CmfAttribute<CmfValue> getCandidate(TransformationContext ctx, String name) {
		return ctx.getAttribute(name);
	}

}