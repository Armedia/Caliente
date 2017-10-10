
package com.armedia.caliente.engine.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.engine.transform.TypedValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionRemoveVariable.t", propOrder = {
	"comparison", "name"
})
public class VariableRemove extends AbstractTransformValueVariable {

	@Override
	protected void applyTransformation(ObjectContext ctx, TypedValue candidate) {
		ctx.getVariables().remove(candidate.getName());
	}

}