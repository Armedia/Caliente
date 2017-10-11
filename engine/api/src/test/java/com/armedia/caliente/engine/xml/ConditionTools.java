package com.armedia.caliente.engine.xml;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.ConditionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;

public class ConditionTools {

	public static final Condition COND_TRUE = new Condition() {
		@Override
		public boolean check(DynamicElementContext ctx) {
			return true;
		}
	};

	public static final Condition COND_FALSE = new Condition() {
		@Override
		public boolean check(DynamicElementContext ctx) {
			return false;
		}
	};

	public static final Condition COND_FAIL = new Condition() {
		@Override
		public boolean check(DynamicElementContext ctx) throws ConditionException {
			throw new ConditionException("Expected failure");
		}
	};
}