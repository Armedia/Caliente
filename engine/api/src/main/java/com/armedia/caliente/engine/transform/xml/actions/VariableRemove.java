
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionRemoveVariable.t", propOrder = {
	"comparison", "name"
})
public class VariableRemove extends AbstractTransformValueVariable {

	@Override
	protected void applyTransformation(TransformationContext ctx, CmfProperty<CmfValue> candidate) {
		ctx.removeVariable(candidate.getName());
	}

}