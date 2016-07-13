package com.armedia.cmf.engine.tools;

import com.armedia.commons.utilities.Tools;

public class ValueEqualsExpression implements BooleanExpression {

	private final String string;
	private final Object value;

	public ValueEqualsExpression(String string, Object value) {
		this.string = string;
		this.value = value;
	}

	@Override
	public boolean evaluate(BooleanContext c) {
		return Tools.equals(this.value, c.getValue(this.string));
	}
}