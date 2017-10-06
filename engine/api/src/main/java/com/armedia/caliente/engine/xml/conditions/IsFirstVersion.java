
package com.armedia.caliente.engine.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.transform.TypedValue;
import com.armedia.caliente.engine.xml.Condition;
import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsFirstVersion.t")
public class IsFirstVersion implements Condition {

	@Override
	public boolean check(TransformationContext ctx) {
		TypedValue index = ctx.getObject().getPriv().get(IntermediateProperty.VERSION_INDEX.encode());
		Object v = ((index != null) && !index.isEmpty() ? index.getValue() : null);
		if (v == null) { return true; }
		// Is it the number 1?
		try {
			return (Integer.valueOf(v.toString()) == 1);
		} catch (NumberFormatException e) {
			return false;
		}
	}

}