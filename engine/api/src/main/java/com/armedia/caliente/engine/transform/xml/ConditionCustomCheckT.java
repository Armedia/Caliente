
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ConditionFactory;
import com.armedia.caliente.engine.transform.DynamicTransformationElements;
import com.armedia.caliente.engine.transform.RuntimeTransformationException;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCustomCheck.t")
public class ConditionCustomCheckT extends ConditionExpressionT {

	@Override
	public boolean check(TransformationContext ctx) {
		String className = Tools.toString(evaluate(ctx));
		if (className == null) { throw new RuntimeTransformationException(
			String.format("The given %s expression did not return a string value: %s", getLang(), getValue())); }

		ConditionFactory factory = DynamicTransformationElements.getConditionFactory(className);
		if (factory == null) { throw new RuntimeTransformationException(
			String.format("No factory found for custom condition type [%s]", className)); }
		try {
			Condition condition = factory.acquireInstance();
			try {
				return condition.check(ctx);
			} finally {
				factory.releaseInstance(condition);
			}
		} catch (Exception e) {
			throw new RuntimeTransformationException(
				String.format("Exception caught while trying to evaluate a custom condition of type [%s]", className),
				e);
		}
	}

}