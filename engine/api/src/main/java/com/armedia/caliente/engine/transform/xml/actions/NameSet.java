
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetName.t", propOrder = {
	"value"
})
public class NameSet extends AbstractSingleValueSet {

	@Override
	protected void setNewValue(TransformationContext ctx, String newValue) {
		ctx.getObject().setName(newValue);
	}

}