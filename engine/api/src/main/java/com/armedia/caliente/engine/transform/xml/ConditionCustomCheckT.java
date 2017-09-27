
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCustomCheck.t")
public class ConditionCustomCheckT extends ConditionExpressionT {

	private static final DynamicElementRegistry<Condition> CONDITIONS = new DynamicElementRegistry<>(
		Condition.class);

	@Override
	public boolean check(TransformationContext ctx) {
		String className = Tools.toString(evaluate(ctx));
		if (className == null) { throw new RuntimeTransformationException(
			String.format("The given %s expression did not return a string value: %s", getLang(), getValue())); }

		try {
			DynamicElementFactory<Condition> factory = ConditionCustomCheckT.CONDITIONS.getFactory(className);
			Condition condition = factory.acquireInstance();
			try {
				return condition.check(ctx);
			} finally {
				factory.releaseInstance(condition);
			}
		} catch (Exception e) {
			throw new RuntimeTransformationException(e);
		}
	}

}