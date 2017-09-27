
package com.armedia.caliente.engine.transform.xml;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasAttribute.t")
public class ConditionHasAttributeT extends ConditionExpressionComparisonT {

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		final CmfObject<V> object = ctx.getObject();
		final String comparand = Tools.toString(evaluate(ctx));
		final Comparison comparison = getComparison();

		Set<String> names = object.getAttributeNames();
		if (comparison == Comparison.EQ) {
			// Shortcut - look for the attribute explicitly
			return names.contains(comparand);
		}

		// Bad news...we have to go through all names and apply the comparison
		for (String s : names) {
			if (comparison.check(comparand, s)) { return true; }
		}
		// No successful match...
		return false;
	}

}