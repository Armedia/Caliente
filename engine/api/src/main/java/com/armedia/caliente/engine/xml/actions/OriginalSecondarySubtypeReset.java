
package com.armedia.caliente.engine.xml.actions;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.xml.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionResetOriginalSecondarySubtypes.t")
public class OriginalSecondarySubtypeReset extends ConditionalAction {

	@Override
	protected void applyTransformation(TransformationContext ctx) throws TransformationException {
		Set<String> secondaries = ctx.getObject().getSecondarySubtypes();
		secondaries.clear();
		secondaries.addAll(ctx.getObject().getOriginalSecondarySubtypes());
	}

}