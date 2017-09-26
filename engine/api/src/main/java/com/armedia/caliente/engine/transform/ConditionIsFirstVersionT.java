
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsFirstVersion.t")
public class ConditionIsFirstVersionT implements Condition {

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		// TODO Auto-generated method stub
		return false;
	}

}