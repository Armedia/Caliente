
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ObjectContext;
import com.armedia.caliente.engine.dynamic.TypedValue;
import com.armedia.caliente.store.CmfDataType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetVariable.t", propOrder = {
	"name", "type", "value"
})
public class VariableSet extends AbstractSetValue {

	@Override
	protected TypedValue createValue(ObjectContext ctx, String name, CmfDataType type,
		boolean multivalue) {
		TypedValue member = new TypedValue(name, type, multivalue);
		ctx.getVariables().put(name, member);
		return member;
	}

}