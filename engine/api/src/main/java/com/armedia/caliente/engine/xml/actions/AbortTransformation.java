
package com.armedia.caliente.engine.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.xml.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionAbortTransformation.t")
public class AbortTransformation extends ConditionalAction {

	@Override
	protected void applyTransformation(TransformationContext ctx) throws TransformationException {
		throw new TransformationException(
			String.format("The object transformation for %s was explicitly aborted", getObjectDescription(ctx)));
	}

}