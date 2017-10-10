
package com.armedia.caliente.engine.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TypedValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionRemoveAttribute.t", propOrder = {
	"comparison", "name"
})
public class AttributeRemove extends AbstractTransformValueAttribute {

	@Override
	protected void applyTransformation(TransformationContext ctx, TypedValue candidate) {
		ctx.getTransformableObject().getAtt().remove(candidate.getName());
	}

}