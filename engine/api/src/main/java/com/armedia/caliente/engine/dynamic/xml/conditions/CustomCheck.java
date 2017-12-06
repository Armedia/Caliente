
package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.ConditionFactory;
import com.armedia.caliente.engine.dynamic.CustomComponents;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCustomCheck.t")
public class CustomCheck extends AbstractExpressionCondition {

	@Override
	public boolean check(DynamicElementContext ctx) throws ConditionException {
		String className = Tools.toString(ConditionTools.eval(this, ctx));
		if (className == null) { throw new ConditionException(
			String.format("The given %s expression did not return a string value: %s", getLang(), getScript())); }

		ConditionFactory factory = CustomComponents.getConditionFactory(className);
		if (factory == null) { throw new ConditionException(
			String.format("No factory found for custom condition type [%s]", className)); }
		try {
			final Condition condition = factory.acquireInstance(className);
			try {
				return condition.check(ctx);
			} finally {
				try {
					factory.releaseInstance(condition);
				} catch (Exception e) {
					this.log.warn("Failed to release a Condition instance of {}", className, e);
				}
			}
		} catch (Exception e) {
			throw new ConditionException(
				String.format("Exception caught while trying to evaluate a custom condition of type [%s]", className),
				e);
		}
	}

}