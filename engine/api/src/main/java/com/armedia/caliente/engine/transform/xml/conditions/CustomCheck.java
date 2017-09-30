
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ConditionFactory;
import com.armedia.caliente.engine.transform.DynamicTransformationElements;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Condition;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCustomCheck.t")
public class CustomCheck extends AbstractExpressionCondition {

	@Override
	public boolean check(TransformationContext ctx) throws TransformationException {
		String className = Tools.toString(Expression.eval(this, ctx));
		if (className == null) { throw new TransformationException(
			String.format("The given %s expression did not return a string value: %s", getLang(), getScript())); }

		ConditionFactory factory = DynamicTransformationElements.getConditionFactory(className);
		if (factory == null) { throw new TransformationException(
			String.format("No factory found for custom condition type [%s]", className)); }
		try {
			Condition condition = factory.acquireInstance(className);
			try {
				return condition.check(ctx);
			} finally {
				factory.releaseInstance(condition);
			}
		} catch (Exception e) {
			throw new TransformationException(
				String.format("Exception caught while trying to evaluate a custom condition of type [%s]", className),
				e);
		}
	}

}