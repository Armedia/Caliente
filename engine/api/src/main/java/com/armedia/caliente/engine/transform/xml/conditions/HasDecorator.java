
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Expression;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasDecorator.t")
public class HasDecorator extends AbstractExpressionComparison {

	@Override
	public boolean check(TransformationContext ctx) throws TransformationException {
		Object decorator = Expression.eval(this, ctx);
		if (decorator == null) { return false; }
		return ctx.getObject().getDecorators().contains(decorator);
	}

}