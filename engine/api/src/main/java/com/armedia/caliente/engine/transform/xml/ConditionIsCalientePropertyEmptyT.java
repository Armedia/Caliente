
package com.armedia.caliente.engine.transform.xml;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsCalientePropertyEmpty.t")
public class ConditionIsCalientePropertyEmptyT extends ConditionExpressionComparisonT {

	@Override
	public boolean check(TransformationContext ctx) {
		final String comparand = Tools.toString(evaluate(ctx));
		final Comparison comparison = getComparison();

		Set<String> names = ctx.getPropertyNames();
		if (comparison == Comparison.EQ) {
			// Shortcut!! Look for only one!
			CmfProperty<CmfValue> prop = ctx.getProperty(comparand);
			return ((prop == null) || !prop.hasValues());
		}
		for (String s : names) {
			if (comparison.check(CmfDataType.STRING, comparand, s)) {
				// This property matches...if this one is empty, we're done!
				if (!ctx.getProperty(s).hasValues()) { return true; }
			}
		}
		// None of the matching properties was empty...so this is false
		return false;
	}

}