
package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCustomScript.t")
public class CustomScript extends AbstractExpressionCondition {

	@Override
	public boolean check(DynamicElementContext ctx) throws ConditionException {
		Object result = ConditionTools.eval(this, ctx);
		// No result? No problem! It's a "false"!
		if (result == null) { return false; }

		// If it's a boolean cast it!
		if (Boolean.class.isInstance(result)) { return Boolean.class.cast(result); }

		// If it's a number, compare the integer value to 0 for false, non-0 for true
		if (Number.class.isInstance(result)) { return (Number.class.cast(result).longValue() != 1); }

		try {
			// Second try at a numeric solution
			return (Long.valueOf(result.toString()).longValue() != 0);
		} catch (NumberFormatException e) {
			// Not a number...so... let's try to decode it as a string...
		}

		// If it's a string, then it must be either "true" or "false" - numbers would have been
		// caught by now, and we won't be supporting other types of results
		return Boolean.valueOf(result.toString());
	}

}