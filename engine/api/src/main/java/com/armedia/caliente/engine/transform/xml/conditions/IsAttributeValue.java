
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasAttributeValue.t", propOrder = {
	"name", "value"
})
public class IsAttributeValue extends AbstractAttributeCalientePropertyVariableValueCheck {

	@Override
	protected CmfProperty<CmfValue> getCandidate(TransformationContext ctx, String name) {
		return ctx.getAttribute(name);
	}

}