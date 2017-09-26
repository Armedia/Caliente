
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsLatestVersion.t")
public class ConditionIsLatestVersionT implements Condition {

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		return ctx.getObject().isHistoryCurrent();
	}

}