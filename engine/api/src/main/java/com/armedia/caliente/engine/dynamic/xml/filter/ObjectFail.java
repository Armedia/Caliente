
package com.armedia.caliente.engine.dynamic.xml.filter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "filterFailObject.t")
public class ObjectFail extends ConditionalAction {

	@Override
	protected void executeAction(DynamicElementContext ctx) throws ActionException {
		throw new ActionException(
			String.format("Explicitly failed processing %s", ctx.getBaseObject().getDescription()));
	}

}