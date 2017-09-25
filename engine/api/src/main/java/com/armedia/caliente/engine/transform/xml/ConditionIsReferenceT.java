
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsReference.t")
public class ConditionIsReferenceT implements Condition {

	@Override
	public <V> boolean evaluate(TransformationContext<V> ctx) {
		return true; // ctx.getObject().isReference();
	}

}
