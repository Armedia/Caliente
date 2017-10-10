
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ObjectContext;
import com.armedia.caliente.engine.dynamic.TypedValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionRemoveAttribute.t", propOrder = {
	"comparison", "name"
})
public class AttributeRemove extends AbstractTransformValueAttribute {

	@Override
	protected void applyTransformation(ObjectContext ctx, TypedValue candidate) {
		ctx.getTransformableObject().getAtt().remove(candidate.getName());
	}

}