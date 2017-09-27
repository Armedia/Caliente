
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsFirstVersion.t")
public class ConditionIsFirstVersionT implements Condition {

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		// TODO implement this condition
		// Maybe use the version index properties?
		return false;
	}

}