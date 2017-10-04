
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
@XmlType(name = "conditionHasOriginalDecorator.t")
public class HasOriginalDecorator extends AbstractExpressionComparison {

	@Override
	public boolean check(TransformationContext ctx) throws TransformationException {
		Object decorator = Expression.eval(this, ctx);
		if (decorator == null) { return false; }
		final Comparison comp = getComparison();
		for (String d : ctx.getObject().getOriginalDecorators()) {
			if (comp.check(CmfDataType.STRING, d, decorator.toString())) { return true; }
		}
		return false;
	}

}