package com.armedia.cmf.engine.tools;

public final class NotExpression<C extends BooleanContext> implements BooleanExpression<C> {

	private final BooleanExpression<C> expression;

	public NotExpression(BooleanExpression<C> expression) {
		if (expression == null) { throw new IllegalArgumentException("Must provide a non-null expression to negate"); }
		this.expression = expression;
	}

	@Override
	public boolean evaluate(C c) {
		return !this.expression.evaluate(c);
	}

	@Override
	public final String toString() {
		return String.format("NOT[%s]", this.expression);
	}
}