
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.ObjectContext;
import com.armedia.caliente.engine.dynamic.jaxb.ConditionalAction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionFailObject.t")
public class ObjectFail extends ConditionalAction {

	@Override
	protected void applyTransformation(ObjectContext ctx) throws ActionException {
		throw new ActionException("Object explicitly failed");
	}

}