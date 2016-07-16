package com.armedia.cmf.engine.tools;

public class NameExistsExpression<C extends BooleanContext> implements BooleanExpression<C> {

	private final String name;

	public NameExistsExpression(String name) {
		this.name = name;
	}

	public final String getName() {
		return this.name;
	}

	@Override
	public boolean evaluate(C c) {
		return c.hasName(this.name);
	}
}