
package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.store.CmfValue;

@XmlTransient
public abstract class AbstractSingleValueComparison extends AbstractExpressionComparison {

	protected abstract CmfValue.Type getCandidateType(DynamicElementContext ctx);

	protected abstract Object getCandidateValue(DynamicElementContext ctx);

	@Override
	public final boolean check(DynamicElementContext ctx) throws ConditionException {
		Object comparand = ConditionTools.eval(this, ctx);
		if (comparand == null) { throw new ConditionException("No value given to compare against"); }
		return getComparison().check(getCandidateType(ctx), getCandidateValue(ctx), comparand);
	}

}