
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCustomCheck.t")
public class ConditionCustomCheckT extends ConditionExpressionT {

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		String className = Tools.toString(evaluate(ctx));
		if (className == null) { throw new TransformationException(
			String.format("The given %s expression did not return a string value: %s", getLang(), getValue())); }

		try {
			Class<?> c = Class.forName(className);

			if (Condition.class.isAssignableFrom(c)) {
				// It's a condition, so try to get an instance right off the bat!
			} else if (ConditionFactory.class.isAssignableFrom(c)) {
				// It's a factory, so build it and try to
			}

			return false;
		} catch (ClassNotFoundException e) {
			throw new TransformationException(e);
		}
	}

}