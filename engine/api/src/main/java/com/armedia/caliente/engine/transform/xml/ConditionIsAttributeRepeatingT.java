
package com.armedia.caliente.engine.transform.xml;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsAttributeRepeating.t")
public class ConditionIsAttributeRepeatingT extends ConditionExpressionComparisonT {

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		final CmfObject<V> object = ctx.getObject();
		final String comparand = Tools.toString(evaluate(ctx));
		final Comparison comparison = getComparison();

		Set<String> names = object.getAttributeNames();
		if (comparison == Comparison.EQ) {
			// Shortcut!! Look for only one!
			CmfAttribute<V> att = object.getAttribute(comparand);
			return ((att != null) && att.isRepeating());
		}
		for (String s : names) {
			if (comparison.check(comparand, s)) {
				// This attribute matches...if this one is repeating, we're done!
				if (object.getAttribute(s).isRepeating()) { return true; }
			}
		}
		// None of the matching attributes was repeating...so this is false
		return false;
	}

}