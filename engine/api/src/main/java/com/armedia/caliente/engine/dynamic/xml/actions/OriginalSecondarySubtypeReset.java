
package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionResetOriginalSecondarySubtypes.t")
public class OriginalSecondarySubtypeReset extends ConditionalAction {

	@Override
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		Set<String> secondaries = ctx.getDynamicObject().getSecondarySubtypes();
		secondaries.clear();
		secondaries.addAll(ctx.getDynamicObject().getOriginalSecondarySubtypes());
	}

}