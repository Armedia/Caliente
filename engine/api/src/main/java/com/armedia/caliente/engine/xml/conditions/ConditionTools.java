package com.armedia.caliente.engine.xml.conditions;

import javax.script.ScriptException;

import com.armedia.caliente.engine.transform.ConditionException;
import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.engine.xml.Expression;

class ConditionTools {

	private ConditionTools() {
	}

	public static Object eval(Expression e, ObjectContext ctx) throws ConditionException {
		try {
			return Expression.eval(e, ctx);
		} catch (ScriptException ex) {
			throw new ConditionException(String.format("Exception raised evaluating the expression :%n%s%n", e), ex);
		}
	}
}