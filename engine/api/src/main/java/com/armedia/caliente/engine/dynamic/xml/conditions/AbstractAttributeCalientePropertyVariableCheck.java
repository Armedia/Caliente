
package com.armedia.caliente.engine.dynamic.xml.conditions;

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractAttributeCalientePropertyVariableCheck extends AbstractExpressionComparison {

	private static final DynamicValue NULL = null;

	protected abstract Map<String, DynamicValue> getCandidateValues(DynamicElementContext ctx);

	protected abstract boolean check(DynamicValue candidate);

	@Override
	public final boolean check(DynamicElementContext ctx) throws ConditionException {
		final String comparand = Tools.toString(ConditionTools.eval(this, ctx));
		final Comparison comparison = getComparison();
		final Map<String, DynamicValue> values = getCandidateValues(ctx);

		if (comparison == Comparison.EQ) {
			// Shortcut!! Look for only one candidate!
			return check(values.get(comparand));
		}

		// Need to find a matching candidate...
		for (String s : values.keySet()) {
			if (comparison.check(CmfDataType.STRING, s, comparand)) {
				// This candidate matches...if this one is empty, we're done!
				if (check(values.get(s))) { return true; }
			}
		}

		// None of the matching candidates fulfilled the check...so the result is whatever the NULL
		// value compares to
		return check(AbstractAttributeCalientePropertyVariableCheck.NULL);
	}

}