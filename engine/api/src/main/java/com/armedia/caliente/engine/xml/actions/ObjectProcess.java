
package com.armedia.caliente.engine.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationCompletedException;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.xml.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionProcessObject.t")
public class ObjectProcess extends ConditionalAction {

	@Override
	protected void applyTransformation(TransformationContext ctx) {
		throw new TransformationCompletedException();
	}

}