
package com.armedia.caliente.engine.dynamic.xml.filter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.filter.ObjectRejectedByFilterException;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "filterRejectObject.t")
public class ObjectReject extends ConditionalAction {

	@Override
	protected void executeAction(DynamicElementContext ctx) {
		throw new ObjectRejectedByFilterException(
			String.format("Explicitly rejected processing %s", ctx.getBaseObject().getDescription()));
	}

}