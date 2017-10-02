
package com.armedia.caliente.engine.transform.xml.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TypedValue;

@XmlTransient
public abstract class AbstractTransformValueAttribute extends AbstractTransformValue {

	@Override
	protected final Map<String, TypedValue> getCandidateValues(TransformationContext ctx) {
		return ctx.getObject().getAtt();
	}

}