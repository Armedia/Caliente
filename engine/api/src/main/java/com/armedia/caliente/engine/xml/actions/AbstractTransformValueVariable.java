
package com.armedia.caliente.engine.xml.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.engine.transform.TypedValue;

@XmlTransient
public abstract class AbstractTransformValueVariable extends AbstractTransformValue {

	@Override
	protected final Map<String, TypedValue> getCandidateValues(ObjectContext ctx) {
		return ctx.getVariables();
	}

}