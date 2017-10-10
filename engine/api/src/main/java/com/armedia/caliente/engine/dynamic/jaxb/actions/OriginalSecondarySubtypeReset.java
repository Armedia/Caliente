
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.ObjectContext;
import com.armedia.caliente.engine.dynamic.jaxb.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionResetOriginalSecondarySubtypes.t")
public class OriginalSecondarySubtypeReset extends ConditionalAction {

	@Override
	protected void applyTransformation(ObjectContext ctx) throws ActionException {
		Set<String> secondaries = ctx.getTransformableObject().getSecondarySubtypes();
		secondaries.clear();
		secondaries.addAll(ctx.getTransformableObject().getOriginalSecondarySubtypes());
	}

}