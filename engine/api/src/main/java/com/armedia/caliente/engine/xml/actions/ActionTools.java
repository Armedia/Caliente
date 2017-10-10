package com.armedia.caliente.engine.xml.actions;

import javax.script.ScriptException;

import com.armedia.caliente.engine.transform.ActionException;
import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.engine.xml.Expression;

class ActionTools {

	private ActionTools() {
		// TODO Auto-generated constructor stub
	}

	public static Object eval(Expression e, ObjectContext ctx) throws ActionException {
		try {
			return Expression.eval(e, ctx);
		} catch (ScriptException ex) {
			throw new ActionException(String.format("Exception raised evaluating the expression :%n%s%n", e), ex);
		}
	}
}