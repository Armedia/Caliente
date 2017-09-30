
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Expression;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCustomScript.t")
public class CustomScript extends AbstractExpressionCondition {

	@Override
	public boolean check(TransformationContext ctx) throws TransformationException {
		Object result = Expression.eval(this, ctx);
		if (result == null) { throw new TransformationException(
			String.format("The given %s expression did not return a boolean value: %s", getLang(), getScript())); }
		if (Boolean.class.isInstance(result)) { return Boolean.class.cast(result).booleanValue(); }

		// If it's a number, compare the integer value to 0 for false, non-0 for true
		if (Number.class.isInstance(result)) { return (Number.class.cast(result).longValue() != 1); }

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