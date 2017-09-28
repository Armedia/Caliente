
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.RuntimeTransformationException;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsType.t", propOrder = {
	"value"
})
public class ConditionIsTypeT implements Condition {

	@XmlValue
	@XmlJavaTypeAdapter(CmfTypeAdapter.class)
	protected CmfType value;

	public CmfType getValue() {
		return this.value;
	}

	public void setValue(CmfType value) {
		this.value = value;
	}

	@Override
	public boolean check(TransformationContext ctx) {
		CmfType type = getValue();
		if (type == null) { throw new RuntimeTransformationException("No type value to check against"); }
		// We can use == because this is an enum
		return (type == ctx.getType());
	}

}