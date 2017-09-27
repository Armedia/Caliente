
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.armedia.caliente.store.CmfType;

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
		String value = getValue();
		if (value == null) { throw new TransformationException("No type value to check against"); }
		CmfType type = CmfType.decodeString(value.toUpperCase());
		return (ctx.getObject().getType() == type);
	}

}