
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ObjectContext;
import com.armedia.caliente.engine.dynamic.TypedValue;
import com.armedia.caliente.store.CmfDataType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetAttribute.t", propOrder = {
	"name", "type", "value"
})
public class AttributeSet extends AbstractSetValue {

	@Override
	protected TypedValue createValue(ObjectContext ctx, String name, CmfDataType type, boolean multivalue) {
		TypedValue member = new TypedValue(name, type, multivalue);
		ctx.getTransformableObject().getAtt().put(name, member);
		return member;
	}

}