
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.store.CmfDataType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetAttribute.t", propOrder = {
	"name", "type", "value"
})
public class AttributeSet extends AbstractSetValue {

	@Override
	protected DynamicValue createValue(DynamicElementContext ctx, String name, CmfDataType type, boolean multivalue) {
		DynamicValue member = new DynamicValue(name, type, multivalue);
		ctx.getTransformableObject().getAtt().put(name, member);
		return member;
	}

}