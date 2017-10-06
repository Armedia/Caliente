
package com.armedia.caliente.engine.xml.conditions;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.xml.Expression;
import com.armedia.caliente.store.CmfDataType;

@XmlTransient
public abstract class AbstractSingleValueComparison extends AbstractExpressionComparison {

	protected abstract CmfDataType getCandidateType(TransformationContext ctx);

	protected abstract Object getCandidateValue(TransformationContext ctx);

	@Override
	public final boolean check(TransformationContext ctx) throws TransformationException {
		Object comparand = Expression.eval(this, ctx);
		if (comparand == null) { throw new TransformationException("No value given to compare against"); }
		return getComparison().check(getCandidateType(ctx), getCandidateValue(ctx), comparand);
	}

}