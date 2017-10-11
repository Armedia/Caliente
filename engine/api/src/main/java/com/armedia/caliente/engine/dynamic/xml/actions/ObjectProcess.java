
package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.transformer.TransformationCompletedException;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionProcessObject.t")
public class ObjectProcess extends ConditionalAction {

	@Override
	protected void applyTransformation(DynamicElementContext ctx) {
		throw new TransformationCompletedException();
	}

}