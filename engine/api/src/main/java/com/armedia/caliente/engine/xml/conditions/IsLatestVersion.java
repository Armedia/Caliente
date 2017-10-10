
package com.armedia.caliente.engine.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.engine.xml.Condition;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsLatestVersion.t")
public class IsLatestVersion implements Condition {

	@Override
	public boolean check(ObjectContext ctx) {
		return ctx.getTransformableObject().isHistoryCurrent();
	}

}