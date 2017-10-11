
package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionRemoveOriginalSecondarySubtypes.t")
public class OriginalSecondarySubtypeRemove extends ConditionalAction {

	@Override
	protected void applyTransformation(DynamicElementContext ctx) throws ActionException {
		Set<String> originals = ctx.getTransformableObject().getOriginalSecondarySubtypes();
		ctx.getTransformableObject().getSecondarySubtypes().removeAll(originals);
	}

}