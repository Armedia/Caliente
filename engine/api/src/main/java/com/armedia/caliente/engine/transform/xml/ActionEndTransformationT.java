
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationCompletedException;
import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionEndTransformation.t")
public class ActionEndTransformationT extends ConditionalActionT {

	@Override
	protected void applyTransformation(TransformationContext ctx) {
		throw new TransformationCompletedException();
	}

}