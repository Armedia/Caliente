package com.armedia.caliente.engine.xml;

import com.armedia.caliente.engine.transform.ConditionException;
import com.armedia.caliente.engine.transform.ObjectContext;

public class ConditionTools {

	public static final Condition COND_TRUE = new Condition() {
		@Override
		public boolean check(ObjectContext ctx) {
			return true;
		}
	};

	public static final Condition COND_FALSE = new Condition() {
		@Override
		public boolean check(ObjectContext ctx) {
			return false;
		}
	};

	public static final Condition COND_FAIL = new Condition() {
		@Override
		public boolean check(ObjectContext ctx) throws ConditionException {
			throw new ConditionException("Expected failure");
		}
	};
}