package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.script.ScriptException;

import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.Expression;

class ConditionTools {

	private ConditionTools() {
	}

	public static Object eval(Expression e, DynamicElementContext ctx) throws ConditionException {
		try {
			return Expression.eval(e, ctx);
		} catch (ScriptException ex) {
			throw new ConditionException(String.format("Exception raised evaluating the expression :%n%s%n", e), ex);
		}
	}
}