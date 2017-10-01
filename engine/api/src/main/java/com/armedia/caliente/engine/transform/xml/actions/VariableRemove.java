
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ObjectDataMember;
import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionRemoveVariable.t", propOrder = {
	"comparison", "name"
})
public class VariableRemove extends AbstractTransformValueVariable {

	@Override
	protected void applyTransformation(TransformationContext ctx, ObjectDataMember candidate) {
		ctx.getVariables().remove(candidate.getName());
	}

}