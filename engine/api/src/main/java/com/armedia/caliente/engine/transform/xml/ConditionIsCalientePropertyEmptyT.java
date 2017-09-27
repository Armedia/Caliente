
package com.armedia.caliente.engine.transform.xml;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsCalientePropertyEmpty.t")
public class ConditionIsCalientePropertyEmptyT extends ConditionExpressionComparisonT {

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		final CmfObject<V> object = ctx.getObject();
		final String comparand = Tools.toString(evaluate(ctx));
		final Comparison comparison = getComparison();

		Set<String> names = object.getAttributeNames();
		if (comparison == Comparison.EQ) {
			// Shortcut!! Look for only one!
			CmfProperty<V> prop = object.getProperty(comparand);
			return ((prop == null) || !prop.hasValues());
		}
		for (String s : names) {
			if (comparison.check(comparand, s)) {
				// This property matches...if this one is empty, we're done!
				if (!object.getProperty(s).hasValues()) { return true; }
			}
		}
		// None of the matching properties was empty...so this is false
		return false;
	}

}