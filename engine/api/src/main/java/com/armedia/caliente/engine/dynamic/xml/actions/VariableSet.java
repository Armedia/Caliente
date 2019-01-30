
package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.store.CmfValueType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetVariable.t", propOrder = {
	"name", "type", "value"
})
public class VariableSet extends AbstractSetValue {

	@Override
	protected DynamicValue createValue(DynamicElementContext ctx, String name, CmfValueType type, boolean multivalue) {
		DynamicValue member = new DynamicValue(name, type, multivalue);
		ctx.getVariables().put(name, member);
		return member;
	}

}