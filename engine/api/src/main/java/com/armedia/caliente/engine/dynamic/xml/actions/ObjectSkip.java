
package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.filter.ObjectFilteredException;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSkipObject.t")
public class ObjectSkip extends ConditionalAction {

	@Override
	protected void executeAction(DynamicElementContext ctx) {
		throw new ObjectFilteredException(
			String.format("Explicitly skipped processing %s", ctx.getBaseObject().getDescription()));
	}

}