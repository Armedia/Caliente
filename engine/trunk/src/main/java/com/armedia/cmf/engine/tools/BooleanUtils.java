package com.armedia.cmf.engine.tools;

public final class BooleanUtils {

	private BooleanUtils() {
	}

	private static final class K implements BooleanExpression<BooleanContext> {

		private final Boolean result;

		K(boolean result) {
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

	private static final BooleanExpression<BooleanContext> TRUE = new K(true);
	private static final BooleanExpression<BooleanContext> FALSE = new K(false);

	public static final <T extends BooleanContext> BooleanExpression<T> getTrue() {
		@SuppressWarnings("unchecked")
		BooleanExpression<T> ret = (BooleanExpression<T>) BooleanUtils.TRUE;
		return ret;
	}

	public static final <T extends BooleanContext> BooleanExpression<T> getFalse() {
		@SuppressWarnings("unchecked")
		BooleanExpression<T> ret = (BooleanExpression<T>) BooleanUtils.FALSE;
		return ret;
	}
}