
package com.armedia.caliente.engine.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.xml.Comparison;
import com.armedia.caliente.engine.xml.Transformations;
import com.armedia.caliente.store.CmfDataType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasOriginalSecondarySubtype.t")
public class HasOriginalSecondarySubtype extends AbstractExpressionComparison {

	@Override
	public boolean check(TransformationContext ctx) throws TransformationException {
		Object secondary = Transformations.eval(this, ctx);
		if (secondary == null) { return false; }
		final Comparison comp = getComparison();
		for (String s : ctx.getObject().getOriginalSecondarySubtypes()) {
			if (comp.check(CmfDataType.STRING, s, secondary.toString())) { return true; }
		}
		return false;
	}

}