
package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;

@XmlTransient
public abstract class AbstractTransformValueAttribute extends AbstractTransformValue {

	@Override
	protected final Map<String, DynamicValue> getCandidateValues(DynamicElementContext ctx) {
		return ctx.getDynamicObject().getAtt();
	}

}