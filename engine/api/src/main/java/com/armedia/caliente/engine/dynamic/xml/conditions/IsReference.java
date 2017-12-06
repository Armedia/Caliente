
package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsReference.t")
public class IsReference implements Condition {

	@Override
	public boolean check(DynamicElementContext ctx) {
		DynamicValue v = ctx.getDynamicObject().getPriv().get(IntermediateProperty.IS_REFERENCE.encode());
		if (v == null) { return false; }
		return Tools.toBoolean(v);
	}

}