
package com.armedia.caliente.engine.transform.xml;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasCalienteProperty.t")
public class ConditionHasCalientePropertyT extends ConditionExpressionComparisonT {

	@Override
	public boolean check(TransformationContext ctx) {
		final String comparand = Tools.toString(evaluate(ctx));
		final Comparison comparison = getComparison();

		Set<String> names = ctx.getPropertyNames();
		if (comparison == Comparison.EQ) {
			// Shortcut - look for the property explicitly
			return names.contains(comparand);
		}

		// Bad news...we have to go through all names and apply the comparison
		for (String s : names) {
			if (comparison.check(CmfDataType.STRING, comparand, s)) { return true; }
		}
		// No successful match...
		return false;
	}
}