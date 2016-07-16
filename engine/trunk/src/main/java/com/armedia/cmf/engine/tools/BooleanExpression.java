package com.armedia.cmf.engine.tools;

public interface BooleanExpression<C extends BooleanContext> {

	public boolean evaluate(C c);

}