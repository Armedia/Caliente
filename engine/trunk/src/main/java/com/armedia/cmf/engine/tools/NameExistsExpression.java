package com.armedia.cmf.engine.tools;

public class NameExistsExpression<C extends BooleanContext> implements BooleanExpression<C> {

	private final String string;

	public NameExistsExpression(String string) {
		this.string = string;
	}

	@Override
	public boolean evaluate(C c) {
		return c.hasName(this.string);
	}
}