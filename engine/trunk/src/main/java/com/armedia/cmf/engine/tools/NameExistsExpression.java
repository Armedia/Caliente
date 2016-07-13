package com.armedia.cmf.engine.tools;

public class NameExistsExpression implements BooleanExpression {

	private final String string;

	public NameExistsExpression(String string) {
		this.string = string;
	}

	@Override
	public boolean evaluate(BooleanContext c) {
		return c.hasName(this.string);
	}
}