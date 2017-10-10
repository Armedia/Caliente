
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.dynamic.ObjectContext;
import com.armedia.caliente.engine.dynamic.TypedValue;

@XmlTransient
public abstract class AbstractTransformValueAttribute extends AbstractTransformValue {

	@Override
	protected final Map<String, TypedValue> getCandidateValues(ObjectContext ctx) {
		return ctx.getTransformableObject().getAtt();
	}

}