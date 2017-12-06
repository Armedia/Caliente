
package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionReplaceSubtype.t", propOrder = {
	"regex", "replacement"
})
public class SubtypeReplace extends AbstractSingleReplace {

	@Override
	protected String getOldValue(DynamicElementContext ctx) {
		return ctx.getDynamicObject().getSubtype();
	}

	@Override
	protected void setNewValue(DynamicElementContext ctx, String newValue) {
		ctx.getDynamicObject().setSubtype(newValue);
	}
}