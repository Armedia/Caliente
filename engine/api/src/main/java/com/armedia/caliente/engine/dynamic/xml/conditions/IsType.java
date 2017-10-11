
package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.xml.CmfTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsType.t", propOrder = {
	"value"
})
public class IsType implements Condition {

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
	public boolean check(DynamicElementContext ctx) throws ConditionException {
		CmfType type = getValue();
		if (type == null) { throw new ConditionException("No type value to check against"); }
		// We can use == because this is an enum
		return (type == ctx.getDynamicObject().getType());
	}

}