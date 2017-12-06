
package com.armedia.caliente.engine.dynamic.xml.filter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.ProcessingCompletedException;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "filterAcceptObject.t")
public class ObjectAccept extends ConditionalAction {

	@Override
	protected void executeAction(DynamicElementContext ctx) {
		throw new ProcessingCompletedException();
	}

}