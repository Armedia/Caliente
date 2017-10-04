
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionReplaceSubtype.t", propOrder = {
	"regex", "replacement"
})
public class SubtypeReplace extends AbstractSingleReplace {

	@Override
	protected String getOldValue(TransformationContext ctx) {
		return ctx.getObject().getSubtype();
	}

	@Override
	protected void setNewValue(TransformationContext ctx, String newValue) {
		ctx.getObject().setSubtype(newValue);
	}
}