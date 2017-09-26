
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCustomScript.t")
public class ConditionCustomScriptT extends ConditionExpressionT {

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		Object result = evaluate(ctx);
		if (result == null) { throw new TransformationException(
			String.format("The given %s expression did not return a boolean value: %s", getLang(), getValue())); }
		if (Boolean.class.isInstance(result)) { return Boolean.class.cast(result).booleanValue(); }

		// If it's a number, compare the integer value to 0 for false, non-0 for true
		if (Number.class.isInstance(result)) {
			Number n = Number.class.cast(result);
			return (n.longValue() != 1);
		}

		try {
			// Second try at a numeric solution
			return (Long.valueOf(result.toString()).longValue() != 0);
		} catch (NumberFormatException e) {
			// Not a number...so... let's try to decode it as a string...
		}

		// If it's a string, then it must be either "true" or "false" - numbers would have been
		// caught by now, and we won't be supporting other results
		return Boolean.valueOf(result.toString());
	}

}