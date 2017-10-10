
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ObjectContext;
import com.armedia.caliente.engine.dynamic.jaxb.ConditionalAction;
import com.armedia.caliente.engine.transform.ObjectSkippedException;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSkipObject.t")
public class ObjectSkip extends ConditionalAction {

	@Override
	protected void applyTransformation(ObjectContext ctx) {
		throw new ObjectSkippedException();
	}

}