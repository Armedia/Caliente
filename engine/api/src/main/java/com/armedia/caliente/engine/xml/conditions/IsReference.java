
package com.armedia.caliente.engine.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.engine.transform.TypedValue;
import com.armedia.caliente.engine.xml.Condition;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsReference.t")
public class IsReference implements Condition {

	@Override
	public boolean check(ObjectContext ctx) {
		TypedValue v = ctx.getTransformableObject().getPriv().get(IntermediateProperty.IS_REFERENCE.encode());
		if (v == null) { return false; }
		return Tools.toBoolean(v);
	}

}