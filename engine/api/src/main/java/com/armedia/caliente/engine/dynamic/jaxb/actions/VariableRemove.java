
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionRemoveVariable.t", propOrder = {
	"comparison", "name"
})
public class VariableRemove extends AbstractTransformValueVariable {

	@Override
	protected void applyTransformation(DynamicElementContext ctx, DynamicValue candidate) {
		ctx.getVariables().remove(candidate.getName());
	}

}