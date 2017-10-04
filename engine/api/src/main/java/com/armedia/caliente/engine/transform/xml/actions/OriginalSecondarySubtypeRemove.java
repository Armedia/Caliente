
package com.armedia.caliente.engine.transform.xml.actions;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionRemoveOriginalSecondarySubtypes.t")
public class OriginalSecondarySubtypeRemove extends ConditionalAction {

	@Override
	protected void applyTransformation(TransformationContext ctx) throws TransformationException {
		Set<String> originals = ctx.getObject().getOriginalSecondarySubtypes();
		ctx.getObject().getSecondarySubtypes().removeAll(originals);
	}

}