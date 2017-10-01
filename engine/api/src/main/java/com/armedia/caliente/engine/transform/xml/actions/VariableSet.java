
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TypedValue;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfDataType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetVariable.t", propOrder = {
	"name", "type", "value"
})
public class VariableSet extends AbstractSetValue {

	@Override
	protected TypedValue createValue(TransformationContext ctx, String name, CmfDataType type,
		boolean multivalue) {
		TypedValue member = new TypedValue(name, type, multivalue);
		ctx.getVariables().put(name, member);
		return member;
	}

}