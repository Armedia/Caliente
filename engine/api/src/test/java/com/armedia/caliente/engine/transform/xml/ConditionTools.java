package com.armedia.caliente.engine.transform.xml;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;

public class ConditionTools {

	public static final Condition COND_TRUE = new Condition() {
		@Override
		public boolean check(TransformationContext ctx) throws TransformationException {
			return true;
		}
	};

	public static final Condition COND_FALSE = new Condition() {
		@Override
		public boolean check(TransformationContext ctx) throws TransformationException {
			return false;
		}
	};

	public static final Condition COND_FAIL = new Condition() {
		@Override
		public boolean check(TransformationContext ctx) throws TransformationException {
			throw new TransformationException("Expected failure");
		}
	};
}