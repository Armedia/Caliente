
package com.armedia.caliente.engine.dynamic.jaxb.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.ObjectContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsLatestVersion.t")
public class IsLatestVersion implements Condition {

	@Override
	public boolean check(ObjectContext ctx) {
		return ctx.getTransformableObject().isHistoryCurrent();
	}

}