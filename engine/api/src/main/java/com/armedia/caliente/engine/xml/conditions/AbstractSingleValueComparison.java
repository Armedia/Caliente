
package com.armedia.caliente.engine.xml.conditions;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.ConditionException;
import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.store.CmfDataType;

@XmlTransient
public abstract class AbstractSingleValueComparison extends AbstractExpressionComparison {

	protected abstract CmfDataType getCandidateType(ObjectContext ctx);

	protected abstract Object getCandidateValue(ObjectContext ctx);

	@Override
	public final boolean check(ObjectContext ctx) throws ConditionException {
		Object comparand = ConditionTools.eval(this, ctx);
		if (comparand == null) { throw new ConditionException("No value given to compare against"); }
		return getComparison().check(getCandidateType(ctx), getCandidateValue(ctx), comparand);
	}

}