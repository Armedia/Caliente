
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionIsOriginalSubtype.t")
public class IsOriginalSubtype extends AbstractExpressionComparison {

	@Override
	public boolean check(TransformationContext ctx) throws TransformationException {
		String subtype = Tools.toString(Expression.eval(this, ctx));
		if (subtype == null) { throw new TransformationException("No value given to compare against"); }
		return getComparison().check(CmfDataType.STRING, ctx.getOriginalSubtype(), subtype);
	}

}