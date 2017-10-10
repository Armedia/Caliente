
package com.armedia.caliente.engine.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ConditionException;
import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.engine.xml.Comparison;
import com.armedia.caliente.store.CmfDataType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasSecondarySubtype.t")
public class HasSecondarySubtype extends AbstractExpressionComparison {

	@Override
	public boolean check(ObjectContext ctx) throws ConditionException {
		Object secondary = ConditionTools.eval(this, ctx);
		if (secondary == null) { return false; }
		final Comparison comp = getComparison();
		for (String s : ctx.getTransformableObject().getSecondarySubtypes()) {
			if (comp.check(CmfDataType.STRING, s, secondary.toString())) { return true; }
		}
		return false;
	}

}