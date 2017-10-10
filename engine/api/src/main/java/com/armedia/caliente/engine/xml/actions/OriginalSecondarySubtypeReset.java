
package com.armedia.caliente.engine.xml.actions;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ActionException;
import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.engine.xml.ConditionalAction;

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