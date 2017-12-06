
package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionAbortTransformation.t")
public class AbortTransformation extends ConditionalAction {

	@Override
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		throw new ActionException(
			String.format("The object transformation for %s was explicitly aborted", getObjectDescription(ctx)));
	}

}