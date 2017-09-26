
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsType.t", propOrder = {
	"value"
})
public class ConditionIsTypeT implements Condition {

	@XmlValue
	protected String value;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		// TODO Auto-generated method stub
		return false;
	}

}