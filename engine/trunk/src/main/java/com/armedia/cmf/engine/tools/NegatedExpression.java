package com.armedia.cmf.engine.tools;

public class NegatedExpression implements BooleanExpression {

	private final BooleanExpression expression;

	public NegatedExpression(BooleanExpression expression) {
		if (expression == null) { throw new IllegalArgumentException("Must provide a non-null expression to negate"); }
		this.expression = expression;
	}

	@Override
	public boolean evaluate(BooleanContext c) {
		return !this.expression.evaluate(c);
	}

}