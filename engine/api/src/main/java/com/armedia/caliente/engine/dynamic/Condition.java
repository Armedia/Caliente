package com.armedia.caliente.engine.dynamic;

public interface Condition {

	public boolean check(ObjectContext ctx) throws ConditionException;

}