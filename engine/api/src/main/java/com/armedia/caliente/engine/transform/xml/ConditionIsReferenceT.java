
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsReference.t")
public class ConditionIsReferenceT implements Condition {

	@Override
	public boolean evaluate(TransformationContext ctx) {
		return true; // ctx.getObject().isReference();
	}

}
