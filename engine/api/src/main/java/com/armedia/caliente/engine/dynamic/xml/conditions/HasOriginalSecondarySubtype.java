
package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.store.CmfValueType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasOriginalSecondarySubtype.t")
public class HasOriginalSecondarySubtype extends AbstractExpressionComparison {

	@Override
	public boolean check(DynamicElementContext ctx) throws ConditionException {
		Object secondary = ConditionTools.eval(this, ctx);
		if (secondary == null) { return false; }
		final Comparison comp = getComparison();
		for (String s : ctx.getDynamicObject().getOriginalSecondarySubtypes()) {
			if (comp.check(CmfValueType.STRING, s, secondary.toString())) { return true; }
		}
		return false;
	}

}