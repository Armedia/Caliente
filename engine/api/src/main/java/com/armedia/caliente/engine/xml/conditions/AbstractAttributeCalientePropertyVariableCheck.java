
package com.armedia.caliente.engine.xml.conditions;

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.TypedValue;
import com.armedia.caliente.engine.xml.Comparison;
import com.armedia.caliente.engine.xml.Expression;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractAttributeCalientePropertyVariableCheck extends AbstractExpressionComparison {

	protected abstract Map<String, TypedValue> getCandidateValues(TransformationContext ctx);

	protected abstract boolean check(TypedValue candidate);

	@Override
	public final boolean check(TransformationContext ctx) throws TransformationException {
		final String comparand = Tools.toString(Expression.eval(this, ctx));
		final Comparison comparison = getComparison();
		final Map<String, TypedValue> values = getCandidateValues(ctx);

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

		// None of the matching candidates fulfilled the check...so this is false
		return false;
	}

}