
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Comparison;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.caliente.store.CmfDataType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasSecondarySubtype.t")
public class HasSecondarySubtype extends AbstractExpressionComparison {

	@Override
	public boolean check(TransformationContext ctx) throws TransformationException {
		Object secondary = Expression.eval(this, ctx);
		if (secondary == null) { return false; }
		final Comparison comp = getComparison();
		for (String s : ctx.getObject().getSecondarySubtypes()) {
			if (comp.check(CmfDataType.STRING, s, secondary.toString())) { return true; }
		}
		return false;
	}

}