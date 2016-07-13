package com.armedia.cmf.engine.tools;

public interface BooleanExpression {
	public static final BooleanExpression TRUE = new ConstantBooleanExpression(true);
	public static final BooleanExpression FALSE = new ConstantBooleanExpression(false);

	public boolean evaluate(BooleanContext c);
}