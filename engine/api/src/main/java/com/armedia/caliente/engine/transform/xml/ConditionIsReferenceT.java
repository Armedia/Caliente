
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsReference.t")
public class ConditionIsReferenceT implements Condition {

	@Override
	public boolean check(TransformationContext ctx) {
		// TODO implement this condition
		return false;
	}

}