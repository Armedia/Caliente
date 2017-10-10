package com.armedia.caliente.engine.dynamic.jaxb.conditions;

import javax.script.ScriptException;

import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.ObjectContext;
import com.armedia.caliente.engine.dynamic.jaxb.Expression;

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