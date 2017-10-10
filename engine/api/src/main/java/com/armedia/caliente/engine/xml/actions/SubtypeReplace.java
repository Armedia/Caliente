
package com.armedia.caliente.engine.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ObjectContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionReplaceSubtype.t", propOrder = {
	"regex", "replacement"
})
public class SubtypeReplace extends AbstractSingleReplace {

	@Override
	protected String getOldValue(ObjectContext ctx) {
		return ctx.getTransformableObject().getSubtype();
	}

	@Override
	protected void setNewValue(ObjectContext ctx, String newValue) {
		ctx.getTransformableObject().setSubtype(newValue);
	}
}