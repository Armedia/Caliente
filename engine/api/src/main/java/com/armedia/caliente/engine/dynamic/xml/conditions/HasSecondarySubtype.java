
package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.store.CmfValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasSecondarySubtype.t")
public class HasSecondarySubtype extends AbstractExpressionComparison {

	@Override
	public boolean check(DynamicElementContext ctx) throws ConditionException {
		Object secondary = ConditionTools.eval(this, ctx);
		if (secondary == null) { return false; }
		final Comparison comp = getComparison();
		for (String s : ctx.getDynamicObject().getSecondarySubtypes()) {
			if (comp.check(CmfValue.Type.STRING, s, secondary.toString())) { return true; }
		}
		return false;
	}

}