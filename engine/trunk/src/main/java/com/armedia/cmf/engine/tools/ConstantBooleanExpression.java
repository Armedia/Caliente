package com.armedia.cmf.engine.tools;

final class ConstantBooleanExpression implements BooleanExpression {

	private final Boolean result;

	ConstantBooleanExpression(boolean result) {
		this.result = result;
	}

	@Override
	public boolean evaluate(BooleanContext c) {
		return this.result;
	}

	@Override
	public final String toString() {
		return this.result.toString();
	}
}