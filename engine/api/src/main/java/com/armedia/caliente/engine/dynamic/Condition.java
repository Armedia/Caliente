package com.armedia.caliente.engine.dynamic;

public interface Condition {

	public boolean check(DynamicElementContext ctx) throws ConditionException;

}