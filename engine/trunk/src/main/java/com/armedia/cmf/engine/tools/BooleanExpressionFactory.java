package com.armedia.cmf.engine.tools;

public interface BooleanExpressionFactory<C extends BooleanContext> {

	public BooleanExpression<C> buildExpression(String identifier);

}