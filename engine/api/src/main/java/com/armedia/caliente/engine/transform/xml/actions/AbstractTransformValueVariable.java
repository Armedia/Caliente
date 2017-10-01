
package com.armedia.caliente.engine.transform.xml.actions;

import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.ObjectDataMember;
import com.armedia.caliente.engine.transform.TransformationContext;

@XmlTransient
public abstract class AbstractTransformValueVariable extends AbstractTransformValue {

	@Override
	protected final Set<String> getCandidateNames(TransformationContext ctx) {
		return ctx.getVariables().keySet();
	}

	@Override
	protected final ObjectDataMember getCandidate(TransformationContext ctx, String name) {
		return ctx.getVariables().get(name);
	}

}