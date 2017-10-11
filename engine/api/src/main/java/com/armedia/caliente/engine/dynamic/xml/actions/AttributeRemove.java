
package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionRemoveAttribute.t", propOrder = {
	"comparison", "name"
})
public class AttributeRemove extends AbstractTransformValueAttribute {

	@Override
	protected void applyTransformation(DynamicElementContext ctx, DynamicValue candidate) {
		ctx.getTransformableObject().getAtt().remove(candidate.getName());
	}

}