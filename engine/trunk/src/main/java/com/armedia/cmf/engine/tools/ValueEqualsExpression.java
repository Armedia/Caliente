package com.armedia.cmf.engine.tools;

import com.armedia.commons.utilities.Tools;

public class ValueEqualsExpression<C extends BooleanContext> implements BooleanExpression<C> {

	private final String string;
	private final Object value;

	public ValueEqualsExpression(String string, Object value) {
		this.string = string;
		this.value = value;
	}

	@Override
	public boolean evaluate(C c) {
		return Tools.equals(this.value, c.getValue(this.string));
	}
}