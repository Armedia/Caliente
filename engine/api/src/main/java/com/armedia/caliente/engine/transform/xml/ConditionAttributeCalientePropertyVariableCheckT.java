
package com.armedia.caliente.engine.transform.xml;

import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class ConditionAttributeCalientePropertyVariableCheckT<T extends CmfProperty<CmfValue>>
	extends ConditionExpressionComparisonT {

	protected abstract Set<String> getCandidateNames(TransformationContext ctx);

	protected abstract T getCandidate(TransformationContext ctx, String name);

	protected abstract boolean check(T candidate);

	@Override
	public final boolean check(TransformationContext ctx) {
		final String comparand = Tools.toString(evaluate(ctx));
		final Comparison comparison = getComparison();

		Set<String> names = getCandidateNames(ctx);
		if (comparison == Comparison.EQ) {
			// Shortcut!! Look for only one candidate!
			return check(getCandidate(ctx, comparand));
		}

		// Need to find a matching candidate...
		for (String s : names) {
			if (comparison.check(CmfDataType.STRING, s, comparand)) {
				// This candidate matches...if this one is empty, we're done!
				if (check(getCandidate(ctx, s))) { return true; }
			}
		}

		// None of the matching candidates fulfilled the check...so this is false
		return false;
	}

}