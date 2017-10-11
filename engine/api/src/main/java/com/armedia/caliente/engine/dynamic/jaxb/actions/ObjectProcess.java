
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.jaxb.ConditionalAction;
import com.armedia.caliente.engine.transform.TransformationCompletedException;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionProcessObject.t")
public class ObjectProcess extends ConditionalAction {

	@Override
	protected void applyTransformation(DynamicElementContext ctx) {
		throw new TransformationCompletedException();
	}

}