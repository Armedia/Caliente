
package com.armedia.caliente.engine.transform.xml.actions;

import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TypedValue;
import com.armedia.caliente.engine.transform.TransformationContext;

@XmlTransient
public abstract class AbstractTransformValueAttribute extends AbstractTransformValue {

	@Override
	protected final Set<String> getCandidateNames(TransformationContext ctx) {
		return ctx.getObject().getAtt().keySet();
	}

	@Override
	protected final TypedValue getCandidate(TransformationContext ctx, String name) {
		return ctx.getObject().getAtt().get(name);
	}

}