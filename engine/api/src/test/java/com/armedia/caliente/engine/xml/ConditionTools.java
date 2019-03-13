package com.armedia.caliente.engine.xml;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.ConditionException;

public class ConditionTools {

	public static final Condition COND_TRUE = (ctx) -> true;

	public static final Condition COND_FALSE = (ctx) -> false;

	public static final Condition COND_FAIL = (ctx) -> {
		throw new ConditionException("Expected failure");
	};
}