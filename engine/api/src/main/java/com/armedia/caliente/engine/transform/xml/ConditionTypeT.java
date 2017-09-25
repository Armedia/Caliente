
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionType.t", propOrder = {
	"value"
})
public class ConditionTypeT extends SimpleConditionT {

	@Override
	public <V> boolean evaluate(TransformationContext<V> ctx) {
		String value = getValue();
		if (value == null) {
			value = "";
		}
		value = value.trim().toUpperCase();
		CmfType type = CmfType.valueOf(value);
		return (type == ctx.getObject().getType());
	}

}