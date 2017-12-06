package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.script.ScriptException;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.Expression;

class ActionTools {

	private ActionTools() {
	}

	public static Object eval(Expression e, DynamicElementContext ctx) throws ActionException {
		try {
			return Expression.eval(e, ctx);
		} catch (ScriptException ex) {
			throw new ActionException(String.format("Exception raised evaluating the expression :%n%s%n", e), ex);
		}
	}
}